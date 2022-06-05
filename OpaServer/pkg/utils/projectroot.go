package utils

import (
	"path/filepath"
	"runtime"
)

func GetProjectRoot() string {
	_, b, _, _ := runtime.Caller(0)
	currentPath := filepath.Dir(b)
	projectRootPath := filepath.Join(currentPath, "../..")
	return projectRootPath
}
