package global

type Config struct {
	Addr       string `yaml:"addr" json:"addr"`
	Data       string `yaml:"data" json:"data"`
	Debug      bool   `yaml:"debug" json:"debug"`
	Dictionary string `yaml:"dictionary" json:"dictionary"`
	Shard      int    `yaml:"shard" json:"shard"`
	Timeout    int64  `yaml:"timeout" json:"timeout"`
	BufferNum  int    `yaml:"bufferNum" json:"bufferNum"`
}

func GetDefaultConfig() *Config {
	return &Config{
		Addr:       "0.0.0.0:5678",
		Data:       "./data",
		Debug:      false,
		Dictionary: "./data/dictionary.txt",
		Shard:      10,
		Timeout:    600,
		BufferNum:  1000,
	}
}
