package storage

import (
	"sync"
	"time"

	"github.com/syndtr/goleveldb/leveldb"
	"github.com/syndtr/goleveldb/leveldb/filter"
	"github.com/syndtr/goleveldb/leveldb/opt"
)

type LevelDBStorage struct {
	db       *leveldb.DB
	path     string
	mu       sync.RWMutex
	closed   bool
	timeout  int64
	lastTime int64
	count    int64
}

func (s *LevelDBStorage) autoOpenDB() {
	if s.isClosed() {
		s.ReOpen()
	}
	s.lastTime = time.Now().Unix()
}

func NewLevelDBStorage(path string, timeout int64) (*LevelDBStorage, error) {
	db := &LevelDBStorage{
		path:     path,
		closed:   true,
		timeout:  timeout,
		lastTime: time.Now().Unix(),
	}

	go db.task()

	return db, nil
}

func (s *LevelDBStorage) task() {
	if s.timeout == -1 {
		return
	}
	for {
		if !s.isClosed() && time.Now().Unix()-s.lastTime > s.timeout {
			s.Close()
		}
		time.Sleep(time.Duration(5) * time.Second)
	}
}

func (s *LevelDBStorage) Close() error {
	if s.isClosed() {
		return nil
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	err := s.db.Close()
	if err != nil {
		return err
	}
	s.closed = true
	//s.mu.Unlock()
	return nil
}

func (s *LevelDBStorage) isClosed() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.closed
}

func OpenDB(path string) (*leveldb.DB, error) {
	o := &opt.Options{
		Filter: filter.NewBloomFilter(10),
	}
	db, err := leveldb.OpenFile(path, o)
	return db, err
}

func (s *LevelDBStorage) ReOpen() {
	if !s.isClosed() {
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	db, err := OpenDB(s.path)
	if err != nil {
		panic(err)
	}
	s.db = db
	s.closed = false
	//s.mu.Unlock()
	go s.compute()
}

func (s *LevelDBStorage) compute() {
	var count int64
	iter := s.db.NewIterator(nil, nil)
	for iter.Next() {
		count++
	}
	iter.Release()
	s.count = count
}

func (s *LevelDBStorage) GetCount() int64 {
	if s.isClosed() && s.count == 0 {
		s.ReOpen()
		s.compute()
	}
	return s.count
}

func (s *LevelDBStorage) Get(key []byte) ([]byte, bool) {
	s.autoOpenDB()
	val, err := s.db.Get(key, nil)
	if err != nil {
		return nil, false
	}
	return val, true
}

func (s *LevelDBStorage) Has(key []byte) bool {
	s.autoOpenDB()
	Has, err := s.db.Has(key, nil)
	if err != nil {
		panic(err)
	}
	return Has
}

func (s *LevelDBStorage) Set(key, value []byte) {
	s.autoOpenDB()
	err := s.db.Put(key, value, nil)
	if err != nil {
		panic(err)
	}
}

func (s *LevelDBStorage) Delete(key []byte) error {
	s.autoOpenDB()
	return s.db.Delete(key, nil)
}
