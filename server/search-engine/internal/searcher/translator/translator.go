package translator

import (
	"campusmart/search-engine/internal/redis"
	"strings"
	"time"

	"github.com/patrickmn/go-cache"
)

var GoCache = cache.New(5*time.Minute, 10*time.Minute)

func TranslateWordsToEn(words []string) []string {
	initHotWords()
	var result []string
	for _, word := range words {
		translated := translateWordToEn(word)
		translated = strings.ToLower(translated)
		result = append(result, translated)
	}
	return result
}

func translateWordToEn(word string) string {
	//fmt.Println(word)
	if val, ok := hotWordsToEn[word]; ok {
		return val
	}

	if val, found := GoCache.Get(word); found {
		return val.(string)
	}

	if redis.Rdb != nil {
		if val, err := redis.Rdb.Get(redis.Ctx, word).Result(); err == nil {
			return val
		}
	}

	translated := CallTranslationAPIToEn(word)
	//fmt.Println(translated)

	if translated != "" && translated != word {
		//	fmt.Println(translated, word)
		GoCache.Set(word, translated, cache.DefaultExpiration)
		if redis.Rdb != nil {
			redis.Rdb.Set(redis.Ctx, word, translated, 7*24*time.Hour)
		}
		return translated
	}

	return word
}

func TranslateWordsToCn(words []string) []string {
	initHotWords()
	var result []string
	for _, word := range words {
		translated := translateWordToCn(word)
		result = append(result, translated)
	}
	return result
}

func translateWordToCn(word string) string {
	if val, ok := hotWordsToCn[word]; ok {
		return val
	}

	if val, found := GoCache.Get(word); found {
		return val.(string)
	}

	if redis.Rdb != nil {
		if val, err := redis.Rdb.Get(redis.Ctx, word).Result(); err == nil {
			return val
		}
	}

	translated := CallTranslationAPIToCn(word)
	//fmt.Println(translated)

	if translated != "" && translated != word {
		//fmt.Println(translated, word)
		GoCache.Set(word, translated, cache.DefaultExpiration)
		if redis.Rdb != nil {
			redis.Rdb.Set(redis.Ctx, word, translated, 7*24*time.Hour)
		}
		return translated
	}

	return word
}
