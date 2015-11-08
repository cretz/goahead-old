package lang

import (
	"sync"
	"fmt"
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

func (this *SystemOut) Println(arg0 fmt.Stringer) {
	println(arg0.String())
}