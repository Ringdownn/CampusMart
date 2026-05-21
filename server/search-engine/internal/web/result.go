package web

type Result struct {
	State   bool        `json:"state"`
	Message string      `json:"message,omitempty"`
	Data    interface{} `json:"data,omitempty"`
}

func NewResult(state bool, message string, data interface{}) *Result {
	return &Result{
		State:   state,
		Message: message,
		Data:    data,
	}
}

func Success(data interface{}) *Result {
	return NewResult(true, "success", data)
}

func Error(message string) *Result {
	return NewResult(false, message, nil)
}
