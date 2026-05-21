package service

import (
	"campusmart/search-engine/internal/global"
	"campusmart/search-engine/internal/searcher"
)

type Database struct {
	Container *searcher.Container
}

func NewDatabase() *Database {
	return &Database{
		Container: global.Container,
	}
}

func (db *Database) Show() map[string]*searcher.Engine {
	return db.Container.GetDataBases()
}

func (db *Database) Create(dbName string) *searcher.Engine {
	return db.Container.GetDataBase(dbName)
}

func (db *Database) Drop(dbName string) error {
	if err := db.Container.DropDatabase(dbName); err != nil {
		return err
	}
	return nil
}
