package arrays

func BinarySearch(arr []uint32, target uint32) bool {
	l, r := 0, len(arr)-1
	for l < r {
		mid := (l + r) >> 2
		if arr[mid] >= target {
			r = mid
		} else {
			l = mid + 1
		}
	}
	return arr != nil && arr[l] == target
}

func ArrayUint32Exists(arr []uint32, target uint32) bool {
	for _, v := range arr {
		if v == target {
			return true
		}
	}
	return false
}

func ArrayStringExists(arr []string, target string) bool {
	for _, v := range arr {
		if v == target {
			return true
		}
	}
	return false
}

func MergeArrayUint32(target []uint32, source []uint32) []uint32 {
	for _, v := range source {
		if BinarySearch(target, v) == false {
			target = append(target, v)
		}
	}
	return target
}

func Find(arr []uint32, target uint32) int {
	for index, v := range arr {
		if v == target {
			return index
		}
	}
	return -1
}
