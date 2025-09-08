const config = {
    printWidth: 160,
    tabWidth: 4,
    useTabs: false,
    trailingComma: "none",
    requirePragma: false,
    plugins: [require.resolve("prettier-plugin-java"), require.resolve("@prettier/plugin-xml")]
};

module.exports = config;
