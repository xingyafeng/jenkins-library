import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain
import com.sap.piper.DefaultValueCache

import util.BasePiperTest
import util.JenkinsLoggingRule
import util.JenkinsReadYamlRule
import util.JenkinsShellCallRule
import util.JenkinsStepRule

import util.Rules

public class PrepareDefaultValuesTest extends BasePiperTest {

    private JenkinsStepRule stepRule = new JenkinsStepRule(this)
    private JenkinsLoggingRule loggingRule = new JenkinsLoggingRule(this)
    private ExpectedException thrown = ExpectedException.none()
    private JenkinsReadYamlRule readYamlRule = new JenkinsReadYamlRule(this)


    @Rule
    public RuleChain ruleChain = Rules
        .getCommonRules(this)
        .around(readYamlRule)
        .around(thrown)
        .around(stepRule)
        .around(loggingRule)

    @Before
    public void setup() {
        readYamlRule.registerYaml("./pipeline/default_pipeline_environment.yml", new FileInputStream(new File("test/resources/configs/default_pipeline_environment.yml")))
        readYamlRule.registerYaml("./pipeline/custom.yml", new FileInputStream(new File("test/resources/configs/default_pipeline_environment.yml")))
        helper.registerAllowedMethod("libraryResource", [String], { fileName ->
            switch(fileName) {
                case 'default_pipeline_environment.yml': return "default: 'config'"
                case 'custom.yml': return "custom: 'myConfig'"
                case 'not_found': throw new hudson.AbortException('No such library resource not_found could be found')
                default: return "the:'end'"
            }
        })

    }

    @Test
    public void testDefaultPipelineEnvironmentOnly() {

        stepRule.step.prepareDefaultValues(script: nullScript)

        assert DefaultValueCache.getInstance().getDefaultValues().size() == 1
        assert DefaultValueCache.getInstance().getDefaultValues().default == 'config'
    }

    @Test
    public void testReInitializeOnCustomConfig() {

        def instance = DefaultValueCache.createInstance([key:'value'])

        // existing instance is dropped in case a custom config is provided.
        stepRule.step.prepareDefaultValues(script: nullScript, customDefaults: 'custom.yml')

        // this check is for checking we have another instance
        assert ! instance.is(DefaultValueCache.getInstance())

        // some additional checks that the configuration represented by the new
        // config is fine
        assert DefaultValueCache.getInstance().getDefaultValues().size() == 2
        assert DefaultValueCache.getInstance().getDefaultValues().default == 'config'
        assert DefaultValueCache.getInstance().getDefaultValues().custom == 'myConfig'
    }

    @Test
    public void testNoReInitializeWithoutCustomConfig() {

        def instance = DefaultValueCache.createInstance([key:'value'])

        stepRule.step.prepareDefaultValues(script: nullScript)

        assert instance.is(DefaultValueCache.getInstance())
        assert DefaultValueCache.getInstance().getDefaultValues().size() == 1
        assert DefaultValueCache.getInstance().getDefaultValues().key == 'value'
    }
    // TODO: move to setupCommonPipelineEnvTest
    @Test
    public void testAttemptToLoadNonExistingConfigFile() {

        // Behavior documented here based on reality check
        thrown.expect(hudson.AbortException.class)
        thrown.expectMessage('No such library resource not_found could be found')

        stepRule.step.prepareDefaultValues(script: nullScript, customDefaults: 'not_found')
    }
    // TODO: move to setupCommonPipelineEnvTest
    @Test
    public void testDefaultPipelineEnvironmentWithCustomConfigReferencedAsString() {

        stepRule.step.prepareDefaultValues(script: nullScript, customDefaults: 'custom.yml')

        assert DefaultValueCache.getInstance().getDefaultValues().size() == 2
        assert DefaultValueCache.getInstance().getDefaultValues().default == 'config'
        assert DefaultValueCache.getInstance().getDefaultValues().custom == 'myConfig'
    }
    // TODO move to setupCommonPipelineEnvTest
    @Test
    public void testDefaultPipelineEnvironmentWithCustomConfigReferencedAsList() {

        stepRule.step.prepareDefaultValues(script: nullScript, customDefaults: ['custom.yml'])

        assert DefaultValueCache.getInstance().getDefaultValues().size() == 2
        assert DefaultValueCache.getInstance().getDefaultValues().default == 'config'
        assert DefaultValueCache.getInstance().getDefaultValues().custom == 'myConfig'
    }

    @Test
    public void testAssertNoLogMessageInCaseOfNoAdditionalConfigFiles() {

        stepRule.step.prepareDefaultValues(script: nullScript)

        assert ! loggingRule.log.contains("Loading configuration file 'default_pipeline_environment.yml'")
    }
    // TODO: register file in .pipeline
    @Test
    public void testAssertLogMessageInCaseOfMoreThanOneConfigFile() {

        stepRule.step.prepareDefaultValues(script: nullScript, customDefaults: ['custom.yml'])

        assert loggingRule.log.contains("Loading configuration file 'default_pipeline_environment.yml'")
        assert loggingRule.log.contains("Loading configuration file 'custom.yml'")
    }
}
