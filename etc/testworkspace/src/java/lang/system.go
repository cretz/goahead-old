package lang

import (
	"sync"
	"runtime"
)

type SystemStatic struct{
	init sync.Once
	Out *SystemOut
}

var systemStatic SystemStatic

func System() *SystemStatic {
	systemStatic.init.Do(func() {
		systemStatic.Out = &SystemOut{}
	})
	return &systemStatic
}

type SystemOut struct{}

func (this *SystemOut) Println() {
	if runtime.GOOS == "windows" {
		print("\r\n")
	} else {
		print("\n")
	}
}

func (this *SystemOut) Println__java_lang_String(arg0 *StringInstance) {
	print(*arg0)
	this.Println()
}

func (this *SystemOut) Println__int(arg0 int) {
	print(arg0)
	this.Println()
}

func (this *SystemOut) Println__boolean(arg0 bool) {
	print(arg0)
	this.Println()
}