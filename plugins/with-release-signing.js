// Config plugin: inject a release signingConfig into android/app/build.gradle
// that reads NMV_KEYSTORE / NMV_KEYSTORE_PASS from ~/.gradle/gradle.properties.
// Alias is passed per-app via plugin options.
const { withAppBuildGradle } = require('@expo/config-plugins');

const releaseBlock = (alias) => `
        release {
            if (project.hasProperty('NMV_KEYSTORE')) {
                storeFile     file(NMV_KEYSTORE)
                storePassword NMV_KEYSTORE_PASS
                keyAlias      '${alias}'
                keyPassword   NMV_KEYSTORE_PASS
                storeType     'pkcs12'
            }
        }`;

function injectReleaseSigningConfig(contents, alias) {
  if (/signingConfigs\s*\{[\s\S]*?release\s*\{/.test(contents)) return contents;
  return contents.replace(
    /(signingConfigs\s*\{)/,
    `$1${releaseBlock(alias)}\n`
  );
}

function switchReleaseBuildTypeToReleaseSigning(contents) {
  return contents.replace(
    /(buildTypes\s*\{[\s\S]*?release\s*\{[\s\S]*?signingConfig\s+)signingConfigs\.debug/,
    '$1signingConfigs.release'
  );
}

const withReleaseSigning = (config, options) => {
  const alias = options && options.alias;
  if (!alias) {
    throw new Error("with-release-signing: missing 'alias' in plugin options");
  }
  return withAppBuildGradle(config, (cfg) => {
    let contents = cfg.modResults.contents;
    contents = injectReleaseSigningConfig(contents, alias);
    contents = switchReleaseBuildTypeToReleaseSigning(contents);
    cfg.modResults.contents = contents;
    return cfg;
  });
};

module.exports = withReleaseSigning;
