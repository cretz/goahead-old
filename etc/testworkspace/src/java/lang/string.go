package lang

import (
	"unicode/utf8"
)

type StringInstance string

func NewStringInstance__java_lang_String(str string) *StringInstance {
	inst := StringInstance(str)
	return &inst
}

func (this StringInstance) Length() int {
	return utf8.RuneCountInString(string(this))
}