package cmd

import (
	"github.com/SAP/jenkins-library/pkg/command"
	"github.com/SAP/jenkins-library/pkg/log"
	FileUtils "github.com/SAP/jenkins-library/pkg/piperutils"
	"github.com/SAP/jenkins-library/pkg/telemetry"
)

func pipeline(config pipelineOptions, telemetryData *telemetry.CustomData) {
	// for command execution use Command
	c := command.Command{}
	// reroute command output to logging framework
	c.Stdout(log.Writer())
	c.Stderr(log.Writer())

	// for http calls import  piperhttp "github.com/SAP/jenkins-library/pkg/http"
	// and use a  &piperhttp.Client{} in a custom system
	// Example: step checkmarxExecuteScan.go

	// error situations should stop execution through log.Entry().Fatal() call which leads to an os.Exit(1) in the end
	err := runPipeline(&config, telemetryData, &c)
	if err != nil {
		log.Entry().WithError(err).Fatal("step execution failed")
	}
}

func runPipeline(config *pipelineOptions, telemetryData *telemetry.CustomData, command execRunner) error {

	defaultScripts := map[string]string {"prepareVersion": `#!/usr/bin/env bash

set -ex

./piper artifactPrepareVersion
`,
"build": `#!/usr/bin/env bash

set -ex

./piper mavenBuild
`,
"staticChecks": `#!/usr/bin/env bash

set -ex

./piper mavenExecuteStaticCodeChecks
`}
	files := FileUtils.Files{}
	for name, script := range defaultScripts {

		exists, _ := files.FileExists(".pipeline/" + name + ".sh")
		if !exists {
			_ = files.FileWrite(".pipeline/" + name + ".sh", []byte(script), 777)
		}
	}



	return nil
}
