package utils

import (
	"bytes"
	"encoding/binary"
	"encoding/gob"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"time"
)

func ExecTime(fn func()) float64 {
	start := time.Now()
	fn()
	tc := float64(time.Since(start).Nanoseconds())
	return tc / 1e6
}

func ExecTimeWithError(fn func() error) (float64, error) {
	start := time.Now()
	err := fn()
	tc := float64(time.Since(start).Nanoseconds())
	return tc / 1e6, err
}

func Encoder(data interface{}) []byte {
	if data == nil {
		return nil
	}
	buf := new(bytes.Buffer)
	encoder := gob.NewEncoder(buf)
	err := encoder.Encode(data)
	if err != nil {
		panic(err)
	}
	return buf.Bytes()
}

func Decoder(data []byte, v interface{}) {
	if data == nil {
		return
	}
	buf := bytes.NewBuffer(data)
	decoder := gob.NewDecoder(buf)
	err := decoder.Decode(v)
	if err != nil {
		panic(err)
	}
}

func init() {
	gob.Register([]interface{}{})
	gob.Register(map[string]interface{}{})
}

const (
	c1 = 0xcc9e2d51
	c2 = 0x1b873593
	c3 = 0x85ebca6b
	c4 = 0xc2b2ae35
	r1 = 15
	r2 = 13
	m  = 5
	n  = 0xe6546b64
)

var (
	Seed = uint32(1)
)

func Murmur3(key []byte) (hash uint32) {
	hash = Seed
	iByte := 0
	for ; iByte+4 <= len(key); iByte += 4 {
		k := uint32(key[iByte]) | uint32(key[iByte+1])<<8 | uint32(key[iByte+2])<<16 | uint32(key[iByte+3])<<24
		k *= c1
		k = (k << r1) | (k >> (32 - r1))
		k *= c2
		hash ^= k
		hash = (hash << r2) | (hash >> (32 - r2))
		hash = hash*m + n
	}

	var remainingBytes uint32
	switch len(key) - iByte {
	case 3:
		remainingBytes += uint32(key[iByte+2]) << 16
		fallthrough
	case 2:
		remainingBytes += uint32(key[iByte+1]) << 8
		fallthrough
	case 1:
		remainingBytes += uint32(key[iByte])
		remainingBytes *= c1
		remainingBytes = (remainingBytes << r1) | (remainingBytes >> (32 - r1))
		remainingBytes = remainingBytes * c2
		hash ^= remainingBytes
	}

	hash ^= uint32(len(key))
	hash ^= hash >> 16
	hash *= c3
	hash ^= hash >> 13
	hash *= c4
	hash ^= hash >> 16

	return
}

func StringToInt(s string) uint32 {
	return Murmur3([]byte(s))
}

func Uint32Comparator(a, b interface{}) int {
	UintA := a.(uint32)
	UintB := b.(uint32)
	if UintA < UintB {
		return -1
	} else if UintA > UintB {
		return 1
	}
	return 0
}

func Uint32ToByte(u uint32) []byte {
	b := make([]byte, 4)
	binary.LittleEndian.PutUint32(b, u)
	return b
}

func DeleteArray(arr []uint32, index int) []uint32 {
	return append(arr[:index], arr[index+1:]...)
}

func RemovePunctuation(s string) string {
	reg := regexp.MustCompile(`\p{P}+`)
	return reg.ReplaceAllString(s, "")
}

func RemoveSpace(s string) string {
	reg := regexp.MustCompile(`\s+`)
	return reg.ReplaceAllString(s, "")
}

func RemoveEnglish(str string) string {
	// 去除 a-z 和 A-Z 的字符
	reg := regexp.MustCompile(`[a-zA-Z]+`)
	return reg.ReplaceAllString(str, "")
}

// DirSize 计算占用磁盘空间大小
func DirSize(path string) int64 {
	var size int64
	filepath.Walk(path, func(_ string, info os.FileInfo, err error) error {
		if !info.IsDir() {
			size += info.Size()
		}
		return err
	})

	return size
}

func ReleaseAssets(file fs.File, out string) {
	if file == nil {
		return
	}

	if out == "" {
		panic("out is empty")
	}

	//判断out文件是否存在
	if _, err := os.Stat(out); os.IsNotExist(err) {
		//读取文件信息
		fileInfo, err := file.Stat()
		if err != nil {
			panic(err)
		}
		buffer := make([]byte, fileInfo.Size())
		_, err = file.Read(buffer)
		if err != nil {
			panic(err)
		}

		// 读取输出文件目录
		outDir := filepath.Dir(out)
		err = os.MkdirAll(outDir, os.ModePerm)
		if err != nil {
			panic(err)
		}

		//创建文件
		outFile, _ := os.Create(out)
		defer func(outFile *os.File) {
			err := outFile.Close()
			if err != nil {
				panic(err)
			}
		}(outFile)

		err = os.WriteFile(out, buffer, os.ModePerm)
		if err != nil {
			panic(err)
		}
	}
}

func ExtractEnglishWords(text string) []string {
	reg := regexp.MustCompile("[a-zA-Z]+")
	return reg.FindAllString(text, -1)
}
