package npm

import (
	"bytes"
	"github.com/SAP/jenkins-library/pkg/log"
	"io"
	"strings"
)

// RegistryOptions holds the configured urls for npm registries
type RegistryOptions struct {
	DefaultNpmRegistry string
	SapNpmRegistry     string
}

type execRunner interface {
	Stdout(out io.Writer)
	SetEnv(envVars []string)
	RunExecutable(executable string, params ...string) error
}

// SetNpmRegistries configures the given npm registries.
func SetNpmRegistries(options *RegistryOptions, execRunner execRunner) error {
	const sapRegistry = "@sap:registry"
	const npmRegistry = "registry"
	configurableRegistries := []string{npmRegistry, sapRegistry}
	environment := []string{}
	for _, registry := range configurableRegistries {
		var buffer bytes.Buffer
		execRunner.Stdout(&buffer)
		err := execRunner.RunExecutable("npm", "config", "get", registry)
		execRunner.Stdout(log.Writer())
		if err != nil {
			return err
		}
		preConfiguredRegistry := buffer.String()

		if registryIsNonEmpty(preConfiguredRegistry) {
			log.Entry().Info("Discovered pre-configured npm registry " + registry + " with value " + preConfiguredRegistry)
		}

		if registry == npmRegistry && options.DefaultNpmRegistry != "" && registryRequiresConfiguration(preConfiguredRegistry, "https://registry.npmjs.org") {
			log.Entry().Info("npm registry " + registry + " was not configured, setting it to " + options.DefaultNpmRegistry)
			environment = append(environment, "npm_config_"+registry+"="+options.DefaultNpmRegistry)
		}

		if registry == sapRegistry && registryRequiresConfiguration(preConfiguredRegistry, "https://npm.sap.com") {
			log.Entry().Info("npm registry " + registry + " was not configured, setting it to " + options.SapNpmRegistry)
			environment = append(environment, "npm_config_"+registry+"="+options.SapNpmRegistry)
		}
	}

	log.Entry().Info("Setting environment: " + strings.Join(environment, ", "))
	execRunner.SetEnv(environment)
	return nil
}

func registryIsNonEmpty(preConfiguredRegistry string) bool {
	return !strings.HasPrefix(preConfiguredRegistry, "undefined") && len(preConfiguredRegistry) > 0
}

func registryRequiresConfiguration(preConfiguredRegistry, url string) bool {
	return strings.HasPrefix(preConfiguredRegistry, "undefined") || strings.HasPrefix(preConfiguredRegistry, url)
}
