package cmd

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPipelineCommand(t *testing.T) {

	testCmd := PipelineCommand()

	// only high level testing performed - details are tested in step generation procudure
	assert.Equal(t, "pipeline", testCmd.Use, "command name incorrect")

}
