package result

type Result[T any] struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    T      `json:"data"`
}

func OK[T any](data T) Result[T] {
	return Result[T]{
		Code:    200,
		Message: "成功",
		Data:    data,
	}
}

func Error(message string) Result[any] {
	return Result[any]{
		Code:    203,
		Message: message,
		Data:    nil,
	}
}
