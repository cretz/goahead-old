package lang

type String string

func (this String) String() string { return string(this) }