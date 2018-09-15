/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.metalava

import com.android.SdkConstants
import com.android.sdklib.SdkVersionInfo
import com.android.tools.metalava.CompatibilityCheck.CheckRequest
import com.android.tools.metalava.doclava1.Errors
import com.android.utils.SdkUtils.wrap
import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.common.io.Files
import com.intellij.pom.java.LanguageLevel
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/** Global options for the metadata extraction tool */
var options = Options(emptyArray())

private const val MAX_LINE_WIDTH = 90

const val ARGS_COMPAT_OUTPUT = "--compatible-output"
const val ARG_HELP = "--help"
const val ARG_VERSION = "--version"
const val ARG_QUIET = "--quiet"
const val ARG_VERBOSE = "--verbose"
const val ARG_CLASS_PATH = "--classpath"
const val ARG_SOURCE_PATH = "--source-path"
const val ARG_SOURCE_FILES = "--source-files"
const val ARG_API = "--api"
const val ARG_XML_API = "--api-xml"
const val ARG_CONVERT_TO_JDIFF = "--convert-to-jdiff"
const val ARG_PRIVATE_API = "--private-api"
const val ARG_DEX_API = "--dex-api"
const val ARG_PRIVATE_DEX_API = "--private-dex-api"
const val ARG_SDK_VALUES = "--sdk-values"
const val ARG_REMOVED_API = "--removed-api"
const val ARG_REMOVED_DEX_API = "--removed-dex-api"
const val ARG_MERGE_ANNOTATIONS = "--merge-annotations"
const val ARG_INPUT_API_JAR = "--input-api-jar"
const val ARG_EXACT_API = "--exact-api"
const val ARG_STUBS = "--stubs"
const val ARG_DOC_STUBS = "--doc-stubs"
const val ARG_STUBS_SOURCE_LIST = "--write-stubs-source-list"
const val ARG_DOC_STUBS_SOURCE_LIST = "--write-doc-stubs-source-list"
const val ARG_PROGUARD = "--proguard"
const val ARG_EXTRACT_ANNOTATIONS = "--extract-annotations"
const val ARG_EXCLUDE_ANNOTATIONS = "--exclude-annotations"
const val ARG_HIDE_PACKAGE = "--hide-package"
const val ARG_MANIFEST = "--manifest"
const val ARG_MIGRATE_NULLNESS = "--migrate-nullness"
const val ARG_CHECK_COMPATIBILITY = "--check-compatibility"
const val ARG_CHECK_COMPATIBILITY_API_CURRENT = "--check-compatibility:api:current"
const val ARG_CHECK_COMPATIBILITY_API_RELEASED = "--check-compatibility:api:released"
const val ARG_CHECK_COMPATIBILITY_REMOVED_CURRENT = "--check-compatibility:removed:current"
const val ARG_CHECK_COMPATIBILITY_REMOVED_RELEASED = "--check-compatibility:removed:released"
const val ARG_INPUT_KOTLIN_NULLS = "--input-kotlin-nulls"
const val ARG_OUTPUT_KOTLIN_NULLS = "--output-kotlin-nulls"
const val ARG_OUTPUT_DEFAULT_VALUES = "--output-default-values"
const val ARG_ANNOTATION_COVERAGE_STATS = "--annotation-coverage-stats"
const val ARG_ANNOTATION_COVERAGE_OF = "--annotation-coverage-of"
const val ARG_WRITE_CLASS_COVERAGE_TO = "--write-class-coverage-to"
const val ARG_WRITE_MEMBER_COVERAGE_TO = "--write-member-coverage-to"
const val ARG_WARNINGS_AS_ERRORS = "--warnings-as-errors"
const val ARG_LINTS_AS_ERRORS = "--lints-as-errors"
const val ARG_SHOW_ANNOTATION = "--show-annotation"
const val ARG_SHOW_UNANNOTATED = "--show-unannotated"
const val ARG_COLOR = "--color"
const val ARG_NO_COLOR = "--no-color"
const val ARG_OMIT_COMMON_PACKAGES = "--omit-common-packages"
const val ARG_SKIP_JAVA_IN_COVERAGE_REPORT = "--skip-java-in-coverage-report"
const val ARG_NO_BANNER = "--no-banner"
const val ARG_ERROR = "--error"
const val ARG_WARNING = "--warning"
const val ARG_LINT = "--lint"
const val ARG_HIDE = "--hide"
const val ARG_UNHIDE_CLASSPATH_CLASSES = "--unhide-classpath-classes"
const val ARG_ALLOW_REFERENCING_UNKNOWN_CLASSES = "--allow-referencing-unknown-classes"
const val ARG_NO_UNKNOWN_CLASSES = "--no-unknown-classes"
const val ARG_APPLY_API_LEVELS = "--apply-api-levels"
const val ARG_GENERATE_API_LEVELS = "--generate-api-levels"
const val ARG_ANDROID_JAR_PATTERN = "--android-jar-pattern"
const val ARG_CURRENT_VERSION = "--current-version"
const val ARG_CURRENT_CODENAME = "--current-codename"
const val ARG_CURRENT_JAR = "--current-jar"
const val ARG_CHECK_KOTLIN_INTEROP = "--check-kotlin-interop"
const val ARG_PUBLIC = "--public"
const val ARG_PROTECTED = "--protected"
const val ARG_PACKAGE = "--package"
const val ARG_PRIVATE = "--private"
const val ARG_HIDDEN = "--hidden"
const val ARG_NO_DOCS = "--no-docs"
const val ARG_JAVA_SOURCE = "--java-source"
const val ARG_REGISTER_ARTIFACT = "--register-artifact"
const val ARG_INCLUDE_ANNOTATIONS = "--include-annotations"
const val ARG_COPY_ANNOTATIONS = "--copy-annotations"
const val ARG_INCLUDE_ANNOTATION_CLASSES = "--include-annotation-classes"
const val ARG_REWRITE_ANNOTATIONS = "--rewrite-annotations"
const val ARG_INCLUDE_SOURCE_RETENTION = "--include-source-retention"
const val ARG_INCLUDE_SIG_VERSION = "--include-signature-version"
const val ARG_UPDATE_API = "--update-api"
const val ARG_DEX_API_MAPPING = "--dex-api-mapping"
const val ARG_GENERATE_DOCUMENTATION = "--generate-documentation"

class Options(
    args: Array<String>,
    /** Writer to direct output to */
    var stdout: PrintWriter = PrintWriter(OutputStreamWriter(System.out)),
    /** Writer to direct error messages to */
    var stderr: PrintWriter = PrintWriter(OutputStreamWriter(System.err))
) {

    /** Internal list backing [sources] */
    private val mutableSources: MutableList<File> = mutableListOf()
    /** Internal list backing [sourcePath] */
    private val mutableSourcePath: MutableList<File> = mutableListOf()
    /** Internal list backing [classpath] */
    private val mutableClassPath: MutableList<File> = mutableListOf()
    /** Internal list backing [showAnnotations] */
    private val mutableShowAnnotations: MutableList<String> = mutableListOf()
    /** Internal list backing [hideAnnotations] */
    private val mutableHideAnnotations: MutableList<String> = mutableListOf()
    /** Internal list backing [stubImportPackages] */
    private val mutableStubImportPackages: MutableSet<String> = mutableSetOf()
    /** Internal list backing [mergeAnnotations] */
    private val mutableMergeAnnotations: MutableList<File> = mutableListOf()
    /** Internal list backing [annotationCoverageOf] */
    private val mutableAnnotationCoverageOf: MutableList<File> = mutableListOf()
    /** Internal list backing [hidePackages] */
    private val mutableHidePackages: MutableList<String> = mutableListOf()
    /** Internal list backing [skipEmitPackages] */
    private val mutableSkipEmitPackages: MutableList<String> = mutableListOf()
    /** Internal list backing [convertToXmlFiles] */
    private val mutableConvertToXmlFiles: MutableList<Pair<File, File>> = mutableListOf()

    /** Ignored flags we've already warned about - store here such that we don't keep reporting them */
    private val alreadyWarned: MutableSet<String> = mutableSetOf()

    /**
     * Set of arguments to invoke documentation generation tool (arg 0) with, unless --no-docs is also
     * supplied
     */
    var invokeDocumentationToolArguments: Array<String> = emptyArray()

    /**
     * Whether to suppress documentation generation, even if a documentation generator has
     * been configured via ${#ARG_GENERATE_DOCUMENTATION}
     */
    var noDocs = false

    /**
     * Whether metalava is invoked as part of updating the API files. When this is true, metalava
     * should *cancel* various other flags that are also being passed in, such as --check-compatibility.
     * This is there to ease integration in the build system: for a given target, the build system will
     * pass all the applicable flags (--stubs, --api, --check-compatibility, --generate-documentation, etc),
     * and this integration is re-used for the update-api facility where we *only* want to generate the
     * signature files. This avoids having duplicate metalava invocation logic where potentially newly
     * added flags are missing in one of the invocations etc.
     */
    var updateApi = false

    /**
     * Whether signature files should emit in "compat" mode, preserving the various
     * quirks of the previous signature file format -- this will for example use a non-standard
     * modifier ordering, it will call enums interfaces, etc. See the [Compatibility] class
     * for more fine grained control (which is not (currently) exposed as individual command line
     * flags.
     */
    var compatOutput = useCompatMode(args)

    /** Whether nullness annotations should be displayed as ?/!/empty instead of with @NonNull/@Nullable. */
    var outputKotlinStyleNulls = !compatOutput

    /** Whether default values should be included in signature files */
    var outputDefaultValues = !compatOutput

    /** Whether we should omit common packages such as java.lang.* and kotlin.* from signature output */
    var omitCommonPackages = !compatOutput

    /**
     * Whether reading signature files should assume the input is formatted as Kotlin-style nulls
     * (e.g. ? means nullable, ! means unknown, empty means not null)
     */
    var inputKotlinStyleNulls = false

    /** If true, treat all warnings as errors */
    var warningsAreErrors: Boolean = false

    /** If true, treat all API lint warnings as errors */
    var lintsAreErrors: Boolean = false

    /** The list of source roots */
    val sourcePath: List<File> = mutableSourcePath

    /** The list of dependency jars */
    val classpath: List<File> = mutableClassPath

    /** All source files to parse */
    var sources: List<File> = mutableSources

    /** Whether to include APIs with annotations (intended for documentation purposes) */
    var showAnnotations = mutableShowAnnotations

    /**
     * Whether to include unannotated elements if {@link #showAnnotations} is set.
     * Note: This only applies to signature files, not stub files.
     */
    var showUnannotated = false

    /** Whether to validate the API for Kotlin interop */
    var checkKotlinInterop = false

    /** Packages to include (if null, include all) */
    var stubPackages: PackageFilter? = null

    /** Packages to import (if empty, include all) */
    var stubImportPackages: Set<String> = mutableStubImportPackages

    /** Packages to exclude/hide */
    var hidePackages = mutableHidePackages

    /** Packages that we should skip generating even if not hidden; typically only used by tests */
    var skipEmitPackages = mutableSkipEmitPackages

    var showAnnotationOverridesVisibility: Boolean = false

    /** Annotations to hide */
    var hideAnnotations = mutableHideAnnotations

    /** Whether to report warnings and other diagnostics along the way */
    var quiet = false

    /** Whether to report extra diagnostics along the way (note that verbose isn't the same as not quiet) */
    var verbose = false

    /** If set, a directory to write stub files to. Corresponds to the --stubs/-stubs flag. */
    var stubsDir: File? = null

    /** If set, a directory to write documentation stub files to. Corresponds to the --stubs/-stubs flag. */
    var docStubsDir: File? = null

    /** If set, a source file to write the stub index (list of source files) to. Can be passed to
     * other tools like javac/javadoc using the special @-syntax. */
    var stubsSourceList: File? = null

    /** If set, a source file to write the doc stub index (list of source files) to. Can be passed to
     * other tools like javac/javadoc using the special @-syntax. */
    var docStubsSourceList: File? = null

    /** Proguard Keep list file to write */
    var proguard: File? = null

    /** If set, a file to write an API file to. Corresponds to the --api/-api flag. */
    var apiFile: File? = null

    /** Like [apiFile], but with JDiff xml format. */
    var apiXmlFile: File? = null

    /** If set, a file to write the private API file to. Corresponds to the --private-api/-privateApi flag. */
    var privateApiFile: File? = null

    /** If set, a file to write the DEX signatures to. Corresponds to [ARG_DEX_API]. */
    var dexApiFile: File? = null

    /** If set, a file to write all DEX signatures and file locations to. Corresponds to [ARG_DEX_API_MAPPING]. */
    var dexApiMappingFile: File? = null

    /** If set, a file to write the private DEX signatures to. Corresponds to --private-dex-api. */
    var privateDexApiFile: File? = null

    /** Path to directory to write SDK values to */
    var sdkValueDir: File? = null

    /** If set, a file to write extracted annotations to. Corresponds to the --extract-annotations flag. */
    var externalAnnotations: File? = null

    /** For [ARG_COPY_ANNOTATIONS], the source directory to read stub annotations from */
    var privateAnnotationsSource: File? = null

    /** For [ARG_COPY_ANNOTATIONS], the target directory to write converted stub annotations from */
    var privateAnnotationsTarget: File? = null

    /**
     * For [ARG_INCLUDE_ANNOTATION_CLASSES], the directory to copy stub annotation source files into the
     * stubs folder from
     */
    var copyStubAnnotationsFrom: File? = null

    /**
     * For [ARG_INCLUDE_SOURCE_RETENTION], true if we want to include source-retention annotations
     * both in the set of files emitted by [ARG_INCLUDE_ANNOTATION_CLASSES] and into the stubs
     * themselves
     */
    var includeSourceRetentionAnnotations = false

    /** For [ARG_REWRITE_ANNOTATIONS], the jar or bytecode folder to rewrite annotations in */
    var rewriteAnnotations: List<File>? = null

    /** A manifest file to read to for example look up available permissions */
    var manifest: File? = null

    /** If set, a file to write a dex API file to. Corresponds to the --removed-dex-api/-removedDexApi flag. */
    var removedApiFile: File? = null

    /** If set, a file to write an API file to. Corresponds to the --removed-api/-removedApi flag. */
    var removedDexApiFile: File? = null

    /** Whether output should be colorized */
    var color = System.getenv("TERM")?.startsWith("xterm") ?: false

    /** Whether to omit Java and Kotlin runtime library packages from annotation coverage stats */
    var omitRuntimePackageStats = false

    /** Whether to generate annotations into the stubs */
    var generateAnnotations = false

    /**
     * A signature file to migrate nullness data from
     */
    var migrateNullsFrom: File? = null

    /** Private backing list for [compatibilityChecks]] */
    private var mutableCompatibilityChecks = mutableListOf<CheckRequest>()

    /** The list of compatibility checks to run */
    val compatibilityChecks: List<CheckRequest> = mutableCompatibilityChecks

    /** Existing external annotation files to merge in */
    var mergeAnnotations: List<File> = mutableMergeAnnotations

    /** Set of jars and class files for existing apps that we want to measure coverage of */
    var annotationCoverageOf: List<File> = mutableAnnotationCoverageOf

    /** File to write the annotation class coverage report to, if any */
    var annotationCoverageClassReport: File? = null

    /** File to write the annotation member coverage report to, if any */
    var annotationCoverageMemberReport: File? = null

    /** An optional <b>jar</b> file to load classes from instead of from source.
     * This is similar to the [classpath] attribute except we're explicitly saying
     * that this is the complete set of classes and that we <b>should</b> generate
     * signatures/stubs from them or use them to diff APIs with (whereas [classpath]
     * is only used to resolve types.) */
    var apiJar: File? = null

    /** Whether to emit coverage statistics for annotations in the API surface */
    var dumpAnnotationStatistics = false

    /** Only used for tests: Normally we want to treat classes not found as source (e.g. supplied via
     * classpath) as hidden, but for the unit tests (where we're not passing in
     * a complete API surface) this makes the tests more cumbersome.
     * This option lets the testing infrastructure treat these classes differently.
     * To see the what this means in practice, try turning it back on for the tests
     * and see what it does to the results :)
     */
    var hideClasspathClasses = true

    /** Only used for tests: Whether during code filtering we allow referencing super classes
     * etc that are unknown (because they're not included in the codebase) */
    var allowReferencingUnknownClasses = true

    /** Reverse of [allowReferencingUnknownClasses]: Require all classes to be known. This
     * is used when compiling the main SDK itself (which includes definitions for everything,
     * including java.lang.Object.) */
    var noUnknownClasses = false

    /**
     * mapping from API level to android.jar files, if computing API levels
     */
    var apiLevelJars: Array<File>? = null

    /** The api level of the codebase, or -1 if not known/specified */
    var currentApiLevel = -1

    /** API level XML file to generate */
    var generateApiLevelXml: File? = null

    /** Reads API XML file to apply into documentation */
    var applyApiLevelsXml: File? = null

    /** Level to include for javadoc */
    var docLevel = DocLevel.PROTECTED

    /** Whether to include the signature file format version number ([SIGNATURE_FORMAT]) in signature files */
    var includeSignatureFormatVersion: Boolean = !compatOutput

    /**
     * Whether to omit locations for warnings and errors. This is not a flag exposed to users
     * or listed in help; this is intended for the unit test suite, used for example for the
     * test which checks compatibility between signature and API files where the paths vary.
     */
    var omitLocations = false

    /** Directory to write signature files to, if any. */
    var androidJarSignatureFiles: File? = null

    /**
     * The language level to use for Java files, set with [ARG_JAVA_SOURCE]
     */
    var javaLanguageLevel: LanguageLevel = LanguageLevel.JDK_1_8

    /** Map from XML API descriptor file to corresponding artifact id name */
    val artifactRegistrations = ArtifactTagger()

    /** List of signature files to export as JDiff files */
    val convertToXmlFiles = mutableConvertToXmlFiles

    /** Temporary folder to use instead of the JDK default, if any */
    var tempFolder: File? = null

    init {
        // Pre-check whether --color/--no-color is present and use that to decide how
        // to emit the banner even before we emit errors
        if (args.contains(ARG_NO_COLOR)) {
            color = false
        } else if (args.contains(ARG_COLOR) || args.contains("-android")) {
            color = true
        }
        // empty args: only when building initial default Options (options field
        // at the top of this file; replaced once the driver runs and passes in
        // a real argv. Don't print a banner when initializing the default options.)
        if (args.isNotEmpty() && !args.contains(ARG_QUIET) && !args.contains(ARG_NO_BANNER) &&
            !args.contains(ARG_VERSION)
        ) {
            if (color) {
                stdout.print(colorized(BANNER.trimIndent(), TerminalColor.BLUE))
            } else {
                stdout.println(BANNER.trimIndent())
            }
        }
        stdout.println()
        stdout.flush()

        var androidJarPatterns: MutableList<String>? = null
        var currentCodeName: String? = null
        var currentJar: File? = null

        var index = 0
        while (index < args.size) {
            val arg = args[index]

            when (arg) {
                ARG_HELP, "-h", "-?" -> {
                    helpAndQuit(color)
                }

                ARG_QUIET -> {
                    quiet = true; verbose = false
                }

                ARG_VERBOSE -> {
                    verbose = true; quiet = false
                }

                ARG_VERSION -> {
                    throw DriverException(stdout = "$PROGRAM_NAME version: ${Version.VERSION}")
                }

                ARGS_COMPAT_OUTPUT -> compatOutput = true

                // For now we don't distinguish between bootclasspath and classpath
                ARG_CLASS_PATH, "-classpath", "-bootclasspath" ->
                    mutableClassPath.addAll(stringToExistingDirsOrJars(getValue(args, ++index)))

                ARG_SOURCE_PATH, "--sources", "--sourcepath", "-sourcepath" -> {
                    val path = getValue(args, ++index)
                    if (path.endsWith(SdkConstants.DOT_JAVA)) {
                        throw DriverException(
                            "$arg should point to a source root directory, not a source file ($path)"
                        )
                    }
                    mutableSourcePath.addAll(stringToExistingDirsOrJars(path))
                }

                ARG_SOURCE_FILES -> {
                    val listString = getValue(args, ++index)
                    listString.split(",").forEach { path ->
                        mutableSources.addAll(stringToExistingFiles(path))
                    }
                }

                ARG_MERGE_ANNOTATIONS, "--merge-zips" -> mutableMergeAnnotations.addAll(
                    stringToExistingDirsOrFiles(
                        getValue(args, ++index)
                    )
                )

                "-sdkvalues", ARG_SDK_VALUES -> sdkValueDir = stringToNewDir(getValue(args, ++index))
                ARG_API, "-api" -> apiFile = stringToNewFile(getValue(args, ++index))
                ARG_XML_API -> apiXmlFile = stringToNewFile(getValue(args, ++index))
                ARG_DEX_API, "-dexApi" -> dexApiFile = stringToNewFile(getValue(args, ++index))
                ARG_DEX_API_MAPPING, "-apiMapping" -> dexApiMappingFile = stringToNewFile(getValue(args, ++index))

                ARG_PRIVATE_API, "-privateApi" -> privateApiFile = stringToNewFile(getValue(args, ++index))
                ARG_PRIVATE_DEX_API, "-privateDexApi" -> privateDexApiFile = stringToNewFile(getValue(args, ++index))

                ARG_REMOVED_API, "-removedApi" -> removedApiFile = stringToNewFile(getValue(args, ++index))
                ARG_REMOVED_DEX_API, "-removedDexApi" -> removedDexApiFile = stringToNewFile(getValue(args, ++index))

                ARG_EXACT_API, "-exactApi" -> {
                    getValue(args, ++index) // prevent next arg from tripping up parser
                    unimplemented(arg) // Not yet implemented (because it seems to no longer be hooked up in doclava1)
                }

                ARG_MANIFEST, "-manifest" -> manifest = stringToExistingFile(getValue(args, ++index))

                ARG_SHOW_ANNOTATION, "-showAnnotation" -> mutableShowAnnotations.add(getValue(args, ++index))

                ARG_SHOW_UNANNOTATED, "-showUnannotated" -> showUnannotated = true

                "--showAnnotationOverridesVisibility" -> {
                    unimplemented(arg)
                    showAnnotationOverridesVisibility = true
                }

                "--hideAnnotations", "-hideAnnotation" -> mutableHideAnnotations.add(getValue(args, ++index))

                ARG_STUBS, "-stubs" -> stubsDir = stringToNewDir(getValue(args, ++index))
                ARG_DOC_STUBS -> docStubsDir = stringToNewDir(getValue(args, ++index))
                ARG_STUBS_SOURCE_LIST -> stubsSourceList = stringToNewFile(getValue(args, ++index))
                ARG_DOC_STUBS_SOURCE_LIST -> docStubsSourceList = stringToNewFile(getValue(args, ++index))

                ARG_EXCLUDE_ANNOTATIONS -> generateAnnotations = false

                // Note that this only affects stub generation, not signature files.
                // For signature files, clear the compatibility mode
                // (--annotations-in-signatures)
                ARG_INCLUDE_ANNOTATIONS -> generateAnnotations = true

                // Flag used by test suite to avoid including locations in
                // the output when diffing against golden files
                "--omit-locations" -> omitLocations = true

                ARG_PROGUARD, "-proguard" -> proguard = stringToNewFile(getValue(args, ++index))

                ARG_HIDE_PACKAGE, "-hidePackage" -> mutableHidePackages.add(getValue(args, ++index))

                "--stub-packages", "-stubpackages" -> {
                    val packages = getValue(args, ++index)
                    val filter = stubPackages ?: run {
                        val newFilter = PackageFilter()
                        stubPackages = newFilter
                        newFilter
                    }
                    filter.packagePrefixes += packages.split(File.pathSeparatorChar)
                }

                "--stub-import-packages", "-stubimportpackages" -> {
                    val packages = getValue(args, ++index)
                    for (pkg in packages.split(File.pathSeparatorChar)) {
                        mutableStubImportPackages.add(pkg)
                        mutableHidePackages.add(pkg)
                    }
                }

                "--skip-emit-packages" -> {
                    val packages = getValue(args, ++index)
                    mutableSkipEmitPackages += packages.split(File.pathSeparatorChar)
                }

                ARG_PUBLIC, "-public" -> docLevel = DocLevel.PUBLIC
                ARG_PROTECTED, "-protected" -> docLevel = DocLevel.PROTECTED
                ARG_PACKAGE, "-package" -> docLevel = DocLevel.PACKAGE
                ARG_PRIVATE, "-private" -> docLevel = DocLevel.PRIVATE
                ARG_HIDDEN, "-hidden" -> docLevel = DocLevel.HIDDEN

                ARG_INPUT_API_JAR -> apiJar = stringToExistingFile(getValue(args, ++index))

                ARG_EXTRACT_ANNOTATIONS -> externalAnnotations = stringToNewFile(getValue(args, ++index))
                ARG_COPY_ANNOTATIONS -> {
                    privateAnnotationsSource = stringToExistingDir(getValue(args, ++index))
                    privateAnnotationsTarget = stringToNewDir(getValue(args, ++index))
                }
                ARG_REWRITE_ANNOTATIONS -> rewriteAnnotations = stringToExistingDirsOrJars(getValue(args, ++index))
                ARG_INCLUDE_ANNOTATION_CLASSES -> copyStubAnnotationsFrom = stringToExistingDir(getValue(args, ++index))
                ARG_INCLUDE_SOURCE_RETENTION -> includeSourceRetentionAnnotations = true

                "--previous-api" -> {
                    migrateNullsFrom = stringToExistingFile(getValue(args, ++index))
                    reporter.report(Errors.DEPRECATED_OPTION, null as File?,
                        "--previous-api is deprecated; instead " +
                        "use $ARG_MIGRATE_NULLNESS $migrateNullsFrom")
                }

                ARG_MIGRATE_NULLNESS -> {
                    // See if the next argument specifies the nullness API codebase
                    if (index < args.size - 1) {
                        val nextArg = args[index + 1]
                        if (!nextArg.startsWith("-")) {
                            val file = fileForPath(nextArg)
                            if (file.isFile) {
                                index++
                                migrateNullsFrom = file
                            }
                        }
                    }
                }

                "--current-api" -> {
                    val file = stringToExistingFile(getValue(args, ++index))
                    mutableCompatibilityChecks.add(CheckRequest(file, ApiType.PUBLIC_API, ReleaseType.DEV))
                    reporter.report(Errors.DEPRECATED_OPTION, null as File?,
                        "--current-api is deprecated; instead " +
                            "use $ARG_CHECK_COMPATIBILITY_API_CURRENT")
                }

                ARG_CHECK_COMPATIBILITY -> {
                    // See if the next argument specifies the compatibility check.
                    // Synonymous with ARG_CHECK_COMPATIBILITY_API_CURRENT, though
                    // for backwards compatibility with earlier versions and usages
                    // can also works in conjunction with ARG_CURRENT_API where the
                    // usage was to use ARG_CURRENT_API to point to the API file and
                    // then specify ARG_CHECK_COMPATIBILITY (without an argument) to
                    // indicate that the current api should also be checked for
                    // compatibility.
                    if (index < args.size - 1) {
                        val nextArg = args[index + 1]
                        if (!nextArg.startsWith("-")) {
                            val file = fileForPath(nextArg)
                            if (file.isFile) {
                                index++
                                mutableCompatibilityChecks.add(CheckRequest(file, ApiType.PUBLIC_API, ReleaseType.DEV))
                            }
                        }
                    }
                }

                ARG_CHECK_COMPATIBILITY_API_CURRENT -> {
                    val file = stringToExistingFile(getValue(args, ++index))
                    mutableCompatibilityChecks.add(CheckRequest(file, ApiType.PUBLIC_API, ReleaseType.DEV))
                }

                ARG_CHECK_COMPATIBILITY_API_RELEASED -> {
                    val file = stringToExistingFile(getValue(args, ++index))
                    mutableCompatibilityChecks.add(CheckRequest(file, ApiType.PUBLIC_API, ReleaseType.RELEASED))
                }

                ARG_CHECK_COMPATIBILITY_REMOVED_CURRENT -> {
                    val file = stringToExistingFile(getValue(args, ++index))
                    mutableCompatibilityChecks.add(CheckRequest(file, ApiType.REMOVED, ReleaseType.DEV))
                }

                ARG_CHECK_COMPATIBILITY_REMOVED_RELEASED -> {
                    val file = stringToExistingFile(getValue(args, ++index))
                    mutableCompatibilityChecks.add(CheckRequest(file, ApiType.REMOVED, ReleaseType.RELEASED))
                }

                ARG_ANNOTATION_COVERAGE_STATS -> dumpAnnotationStatistics = true
                ARG_ANNOTATION_COVERAGE_OF -> mutableAnnotationCoverageOf.addAll(
                    stringToExistingDirsOrJars(
                        getValue(args, ++index)
                    )
                )
                ARG_WRITE_CLASS_COVERAGE_TO -> {
                    annotationCoverageClassReport = stringToNewFile(getValue(args, ++index))
                }
                ARG_WRITE_MEMBER_COVERAGE_TO -> {
                    annotationCoverageMemberReport = stringToNewFile(getValue(args, ++index))
                }

                ARG_ERROR, "-error" -> Errors.setErrorLevel(getValue(args, ++index), Severity.ERROR, true)
                ARG_WARNING, "-warning" -> Errors.setErrorLevel(getValue(args, ++index), Severity.WARNING, true)
                ARG_LINT, "-lint" -> Errors.setErrorLevel(getValue(args, ++index), Severity.LINT, true)
                ARG_HIDE, "-hide" -> Errors.setErrorLevel(getValue(args, ++index), Severity.HIDDEN, true)

                ARG_WARNINGS_AS_ERRORS -> warningsAreErrors = true
                ARG_LINTS_AS_ERRORS -> lintsAreErrors = true
                "-werror" -> {
                    // Temporarily disabled; this is used in various builds but is pretty much
                    // never what we want.
                    // warningsAreErrors = true
                }
                "-lerror" -> {
                    // Temporarily disabled; this is used in various builds but is pretty much
                    // never what we want.
                    // lintsAreErrors = true
                }

                ARG_CHECK_KOTLIN_INTEROP -> checkKotlinInterop = true

                ARG_COLOR -> color = true
                ARG_NO_COLOR -> color = false
                ARG_NO_BANNER -> {
                    // Already processed above but don't flag it here as invalid
                }

                ARG_OMIT_COMMON_PACKAGES, "$ARG_OMIT_COMMON_PACKAGES=yes" -> omitCommonPackages = true
                "$ARG_OMIT_COMMON_PACKAGES=no" -> omitCommonPackages = false

                ARG_SKIP_JAVA_IN_COVERAGE_REPORT -> omitRuntimePackageStats = true

                ARG_UNHIDE_CLASSPATH_CLASSES -> hideClasspathClasses = false
                ARG_ALLOW_REFERENCING_UNKNOWN_CLASSES -> allowReferencingUnknownClasses = true
                ARG_NO_UNKNOWN_CLASSES -> noUnknownClasses = true

                // Extracting API levels
                ARG_ANDROID_JAR_PATTERN -> {
                    val list = androidJarPatterns ?: run {
                        val list = arrayListOf<String>()
                        androidJarPatterns = list
                        list
                    }
                    list.add(getValue(args, ++index))
                }
                ARG_CURRENT_VERSION -> {
                    currentApiLevel = Integer.parseInt(getValue(args, ++index))
                    if (currentApiLevel <= 26) {
                        throw DriverException("Suspicious currentApi=$currentApiLevel, expected at least 27")
                    }
                }
                ARG_CURRENT_CODENAME -> {
                    currentCodeName = getValue(args, ++index)
                }
                ARG_CURRENT_JAR -> {
                    currentJar = stringToExistingFile(getValue(args, ++index))
                }
                ARG_GENERATE_API_LEVELS -> {
                    generateApiLevelXml = stringToNewFile(getValue(args, ++index))
                }
                ARG_APPLY_API_LEVELS -> {
                    applyApiLevelsXml = if (args.contains(ARG_GENERATE_API_LEVELS)) {
                        // If generating the API file at the same time, it doesn't have
                        // to already exist
                        stringToNewFile(getValue(args, ++index))
                    } else {
                        stringToExistingFile(getValue(args, ++index))
                    }
                }

                ARG_NO_DOCS, "-nodocs" -> noDocs = true

                ARG_UPDATE_API -> updateApi = true

                ARG_GENERATE_DOCUMENTATION -> {
                    // Digest all the remaining arguments.
                    // Allow "STUBS_DIR" to reference the stubs directory.
                    var prev = ""
                    invokeDocumentationToolArguments = args.slice(++index until args.size).mapNotNull {
                        var argument = it
                        // When generating documentation, use the doc stubs directory rather than the
                        // original source path
                        val docStubsDir = docStubsDir
                        if (docStubsDir != null && (prev == ARG_SOURCE_PATH || prev == "-sourcepath") &&
                            !argument.contains(docStubsDir.path)
                        ) {
                            // Insert the doc stubs as the default place to look for sources
                            argument = docStubsDir.path
                        }
                        prev = it

                        if (argument == "STUBS_DIR" && docStubsDir != null) {
                            docStubsDir.path
                        } else if (argument == "STUBS_DIR" && stubsDir != null) {
                            stubsDir?.path
                        } else if (argument == "DOCS_STUBS_DIR" && docStubsDir != null) {
                            docStubsDir.path
                        } else if (argument == "DOC_STUBS_SOURCE_LIST" && docStubsSourceList != null) {
                            "@${docStubsSourceList?.path}"
                        } else if (argument == "STUBS_SOURCE_LIST" && stubsSourceList != null) {
                            "@${stubsSourceList?.path}"
                        } else if (argument == "STUBS_SOURCE_LIST" && docStubsSourceList != null) {
                            "@${docStubsSourceList?.path}"
                        } else {
                            argument
                        }
                    }.toTypedArray()

                    index = args.size // jump to end of argument loop
                }

                ARG_REGISTER_ARTIFACT, "-artifact" -> {
                    val descriptor = stringToExistingFile(getValue(args, ++index))
                    val artifactId = getValue(args, ++index)
                    artifactRegistrations.register(artifactId, descriptor)
                }

                ARG_CONVERT_TO_JDIFF -> {
                    val signatureFile = stringToExistingFile(getValue(args, ++index))
                    val jDiffFile = stringToNewFile(getValue(args, ++index))
                    mutableConvertToXmlFiles.add(Pair(signatureFile, jDiffFile))
                }

                "--write-android-jar-signatures" -> {
                    val root = stringToExistingDir(getValue(args, ++index))
                    if (!File(root, "prebuilts/sdk").isDirectory) {
                        throw DriverException("$androidJarSignatureFiles does not point to an Android source tree")
                    }
                    androidJarSignatureFiles = root
                }

                "-encoding" -> {
                    val value = getValue(args, ++index)
                    if (value.toUpperCase() != "UTF-8") {
                        throw DriverException("$value: Only UTF-8 encoding is supported")
                    }
                }

                ARG_JAVA_SOURCE, "-source" -> {
                    val value = getValue(args, ++index)
                    val level = LanguageLevel.parse(value)
                    when {
                        level == null -> throw DriverException("$value is not a valid or supported Java language level")
                        level.isLessThan(LanguageLevel.JDK_1_7) -> throw DriverException("$arg must be at least 1.7")
                        else -> javaLanguageLevel = level
                    }
                }

                "--temp-folder" -> {
                    tempFolder = stringToNewOrExistingDir(getValue(args, ++index))
                }

                // Option only meant for tests (not documented); doesn't work in all cases (to do that we'd
                // need JNA to call libc)
                "--pwd" -> {
                    val pwd = stringToExistingDir(getValue(args, ++index)).absoluteFile
                    System.setProperty("user.dir", pwd.path)
                }

                "--noop", "--no-op" -> {
                }

                // Doclava1 flag: Already the behavior in metalava
                "-keepstubcomments" -> {
                }

                // Unimplemented doclava1 flags (no arguments)
                "-quiet",
                "-yamlV2" -> {
                    unimplemented(arg)
                }

                "-android" -> { // partially implemented: Pick up the color hint, but there may be other implications
                    color = true
                    unimplemented(arg)
                }

                "-stubsourceonly" -> {
                    /* noop */
                }

                // Unimplemented doclava1 flags (1 argument)
                "-d" -> {
                    unimplemented(arg)
                    index++
                }

                // Unimplemented doclava1 flags (2 arguments)
                "-since" -> {
                    unimplemented(arg)
                    index += 2
                }

                // doclava1 doc-related flags: only supported here to make this command a drop-in
                // replacement
                "-referenceonly",
                "-devsite",
                "-ignoreJdLinks",
                "-nodefaultassets",
                "-parsecomments",
                "-offlinemode",
                "-gcmref",
                "-metadataDebug",
                "-includePreview",
                "-staticonly",
                "-navtreeonly",
                "-atLinksNavtree" -> {
                    javadoc(arg)
                }

                // doclava1 flags with 1 argument
                "-doclet",
                "-docletpath",
                "-templatedir",
                "-htmldir",
                "-knowntags",
                "-resourcesdir",
                "-resourcesoutdir",
                "-yaml",
                "-apidocsdir",
                "-toroot",
                "-samplegroup",
                "-samplesdir",
                "-dac_libraryroot",
                "-dac_dataname",
                "-title",
                "-proofread",
                "-todo",
                "-overview" -> {
                    javadoc(arg)
                    index++
                }

                // doclava1 flags with two arguments
                "-federate",
                "-federationapi",
                "-htmldir2" -> {
                    javadoc(arg)
                    index += 2
                }

                // doclava1 flags with three arguments
                "-samplecode" -> {
                    javadoc(arg)
                    index += 3
                }

                // doclava1 flag with variable number of arguments; skip everything until next arg
                "-hdf" -> {
                    javadoc(arg)
                    index++
                    while (index < args.size) {
                        if (args[index].startsWith("-")) {
                            break
                        }
                        index++
                    }
                    index--
                }

                else -> {
                    if (arg.startsWith("-J-") || arg.startsWith("-XD")) {
                        // -J: mechanism to pass extra flags to javadoc, e.g.
                        //    -J-XX:-OmitStackTraceInFastThrow
                        // -XD: mechanism to set properties, e.g.
                        //    -XDignore.symbol.file
                        javadoc(arg)
                    } else if (arg.startsWith(ARG_OUTPUT_KOTLIN_NULLS)) {
                        outputKotlinStyleNulls = if (arg == ARG_OUTPUT_KOTLIN_NULLS) {
                            true
                        } else {
                            yesNo(arg.substring(ARG_OUTPUT_KOTLIN_NULLS.length + 1))
                        }
                    } else if (arg.startsWith(ARG_INPUT_KOTLIN_NULLS)) {
                        inputKotlinStyleNulls = if (arg == ARG_INPUT_KOTLIN_NULLS) {
                            true
                        } else {
                            yesNo(arg.substring(ARG_INPUT_KOTLIN_NULLS.length + 1))
                        }
                    } else if (arg.startsWith(ARG_OUTPUT_DEFAULT_VALUES)) {
                        outputDefaultValues = if (arg == ARG_OUTPUT_DEFAULT_VALUES) {
                            true
                        } else {
                            yesNo(arg.substring(ARG_OUTPUT_DEFAULT_VALUES.length + 1))
                        }
                    } else if (arg.startsWith(ARG_OMIT_COMMON_PACKAGES)) {
                        omitCommonPackages = if (arg == ARG_OMIT_COMMON_PACKAGES) {
                            true
                        } else {
                            yesNo(arg.substring(ARG_OMIT_COMMON_PACKAGES.length + 1))
                        }
                    } else if (arg.startsWith(ARGS_COMPAT_OUTPUT)) {
                        compatOutput = if (arg == ARGS_COMPAT_OUTPUT)
                            true
                        else yesNo(arg.substring(ARGS_COMPAT_OUTPUT.length + 1))
                    } else if (arg.startsWith(ARG_INCLUDE_SIG_VERSION)) {
                        includeSignatureFormatVersion = if (arg == ARG_INCLUDE_SIG_VERSION)
                            true
                        else yesNo(arg.substring(ARG_INCLUDE_SIG_VERSION.length + 1))
                    } else if (arg.startsWith("-")) {
                        // Compatibility flag; map to mutable properties in the Compatibility
                        // class and assign it
                        val compatibilityArg = findCompatibilityFlag(arg)
                        if (compatibilityArg != null) {
                            val dash = arg.indexOf('=')
                            val value = if (dash == -1) {
                                true
                            } else {
                                arg.substring(dash + 1).toBoolean()
                            }
                            compatibilityArg.set(compatibility, value)
                        } else {
                            // Some other argument: display usage info and exit

                            val usage = getUsage(includeHeader = false, colorize = color)
                            throw DriverException(stderr = "Invalid argument $arg\n\n$usage")
                        }
                    } else {
                        // All args that don't start with "-" are taken to be filenames
                        mutableSources.addAll(stringToExistingFiles(arg))
                    }
                }
            }

            ++index
        }

        if (generateApiLevelXml != null) {
            if (androidJarPatterns == null) {
                androidJarPatterns = mutableListOf(
                    "prebuilts/tools/common/api-versions/android-%/android.jar",
                    "prebuilts/sdk/%/public/android.jar"
                )
            }
            apiLevelJars = findAndroidJars(androidJarPatterns!!, currentApiLevel, currentCodeName, currentJar)
        }

        // If the caller has not explicitly requested that unannotated classes and
        // members should be shown in the output then only show them if no annotations were provided.
        if (!showUnannotated && showAnnotations.isEmpty()) {
            showUnannotated = true
        }

        if (noUnknownClasses) {
            allowReferencingUnknownClasses = false
        }

        if (updateApi) {
            // We're running in update API mode: cancel other "action" flags; only signature file generation
            // flags count
            annotationCoverageClassReport = null
            annotationCoverageMemberReport = null
            dumpAnnotationStatistics = false
            apiLevelJars = null
            generateApiLevelXml = null
            applyApiLevelsXml = null
            androidJarSignatureFiles = null
            stubsDir = null
            docStubsDir = null
            stubsSourceList = null
            docStubsSourceList = null
            sdkValueDir = null
            externalAnnotations = null
            proguard = null
            noDocs = true
            invokeDocumentationToolArguments = emptyArray()
            checkKotlinInterop = false
            mutableCompatibilityChecks.clear()
            mutableAnnotationCoverageOf.clear()
            artifactRegistrations.clear()
        }

        checkFlagConsistency()
    }

    private fun findCompatibilityFlag(arg: String): KMutableProperty1<Compatibility, Boolean>? {
        val index = arg.indexOf('=')
        val name = arg
            .substring(0, if (index != -1) index else arg.length)
            .removePrefix("--")
            .replace('-', '_')
        val propertyName = SdkVersionInfo.underlinesToCamelCase(name).decapitalize()
        return Compatibility::class.memberProperties
            .filterIsInstance<KMutableProperty1<Compatibility, Boolean>>()
            .find {
                it.name == propertyName
            }
    }

    private fun findAndroidJars(
        androidJarPatterns: List<String>,
        currentApiLevel: Int,
        currentCodeName: String?,
        currentJar: File?
    ): Array<File> {

        @Suppress("NAME_SHADOWING")
        val currentApiLevel = if (currentCodeName != null && "REL" != currentCodeName) {
            currentApiLevel + 1
        } else {
            currentApiLevel
        }

        val apiLevelFiles = mutableListOf<File>()
        apiLevelFiles.add(File("there is no api 0")) // api level 0: dummy, should not be processed
        val minApi = 1

        // Get all the android.jar. They are in platforms-#
        var apiLevel = minApi - 1
        while (true) {
            apiLevel++
            try {
                var jar: File? = null
                if (apiLevel == currentApiLevel) {
                    jar = currentJar
                }
                if (jar == null) {
                    jar = getAndroidJarFile(apiLevel, androidJarPatterns)
                }
                if (jar == null || !jar.isFile) {
                    if (verbose) {
                        stdout.println("Last API level found: ${apiLevel - 1}")
                    }
                    break
                }
                if (verbose) {
                    stdout.println("Found API $apiLevel at ${jar.path}")
                }
                apiLevelFiles.add(jar)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return apiLevelFiles.toTypedArray()
    }

    private fun getAndroidJarFile(apiLevel: Int, patterns: List<String>): File? {
        return patterns
            .map { fileForPath(it.replace("%", Integer.toString(apiLevel))) }
            .firstOrNull { it.isFile }
    }

    private fun yesNo(answer: String): Boolean {
        return when (answer) {
            "yes", "true", "enabled", "on" -> true
            "no", "false", "disabled", "off" -> false
            else -> throw DriverException(stderr = "Unexpected $answer; expected yes or no")
        }
    }

    /** Makes sure that the flag combinations make sense */
    private fun checkFlagConsistency() {
        if (apiJar != null && sources.isNotEmpty()) {
            throw DriverException(stderr = "Specify either $ARG_SOURCE_FILES or $ARG_INPUT_API_JAR, not both")
        }

        if (compatOutput && outputKotlinStyleNulls) {
            throw DriverException(
                stderr = "$ARG_OUTPUT_KOTLIN_NULLS=yes should not be combined with " +
                    "$ARGS_COMPAT_OUTPUT=yes"
            )
        }

        if (compatOutput && outputDefaultValues) {
            throw DriverException(
                stderr = "$ARG_OUTPUT_DEFAULT_VALUES=yes should not be combined with " +
                    "$ARGS_COMPAT_OUTPUT=yes"
            )
        }

        if (compatOutput && includeSignatureFormatVersion) {
            throw DriverException(
                stderr = "$ARG_INCLUDE_SIG_VERSION=yes should not be combined with " +
                    "$ARGS_COMPAT_OUTPUT=yes"
            )
        }
    }

    private fun javadoc(arg: String) {
        if (!alreadyWarned.add(arg)) {
            return
        }
        if (!options.quiet) {
            reporter.report(
                Severity.WARNING, null as String?, "Ignoring javadoc-related doclava1 flag $arg",
                color = color
            )
        }
    }

    private fun unimplemented(arg: String) {
        if (!alreadyWarned.add(arg)) {
            return
        }
        if (!options.quiet) {
            val message = "Ignoring unimplemented doclava1 flag $arg" +
                when (arg) {
                    "-encoding" -> " (UTF-8 assumed)"
                    "-source" -> "  (1.8 assumed)"
                    else -> ""
                }
            reporter.report(Severity.WARNING, null as String?, message, color = color)
        }
    }

    private fun helpAndQuit(colorize: Boolean = color) {
        throw DriverException(stdout = getUsage(colorize = colorize))
    }

    private fun getValue(args: Array<String>, index: Int): String {
        if (index >= args.size) {
            throw DriverException("Missing argument for ${args[index - 1]}")
        }
        return args[index]
    }

    private fun stringToExistingDir(value: String): File {
        val file = fileForPath(value)
        if (!file.isDirectory) {
            throw DriverException("$file is not a directory")
        }
        return file
    }

    @Suppress("unused")
    private fun stringToExistingDirs(value: String): List<File> {
        val files = mutableListOf<File>()
        for (path in value.split(File.pathSeparatorChar)) {
            val file = fileForPath(path)
            if (!file.isDirectory) {
                throw DriverException("$file is not a directory")
            }
            files.add(file)
        }
        return files
    }

    private fun stringToExistingDirsOrJars(value: String): List<File> {
        val files = mutableListOf<File>()
        for (path in value.split(File.pathSeparatorChar)) {
            val file = fileForPath(path)
            if (!file.isDirectory && !(file.path.endsWith(SdkConstants.DOT_JAR) && file.isFile)) {
                throw DriverException("$file is not a jar or directory")
            }
            files.add(file)
        }
        return files
    }

    private fun stringToExistingDirsOrFiles(value: String): List<File> {
        val files = mutableListOf<File>()
        for (path in value.split(File.pathSeparatorChar)) {
            val file = fileForPath(path)
            if (!file.exists()) {
                throw DriverException("$file does not exist")
            }
            files.add(file)
        }
        return files
    }

    private fun stringToExistingFile(value: String): File {
        val file = fileForPath(value)
        if (!file.isFile) {
            throw DriverException("$file is not a file")
        }
        return file
    }

    @Suppress("unused")
    private fun stringToExistingFileOrDir(value: String): File {
        val file = fileForPath(value)
        if (!file.exists()) {
            throw DriverException("$file is not a file or directory")
        }
        return file
    }

    private fun stringToExistingFiles(value: String): List<File> {
        val files = mutableListOf<File>()
        value.split(File.pathSeparatorChar)
            .map { fileForPath(it) }
            .forEach { file ->
                if (file.path.startsWith("@")) {
                    // File list; files to be read are stored inside. SHOULD have been one per line
                    // but sadly often uses spaces for separation too (so we split by whitespace,
                    // which means you can't point to files in paths with spaces)
                    val listFile = File(file.path.substring(1))
                    if (!listFile.isFile) {
                        throw DriverException("$listFile is not a file")
                    }
                    val contents = Files.asCharSource(listFile, Charsets.UTF_8).read()
                    val pathList = Splitter.on(CharMatcher.whitespace()).trimResults().omitEmptyStrings().split(
                        contents
                    )
                    pathList.asSequence().map { File(it) }.forEach {
                        if (!it.isFile) {
                            throw DriverException("$it is not a file")
                        }
                        files.add(it)
                    }
                } else {
                    if (!file.isFile) {
                        throw DriverException("$file is not a file")
                    }
                    files.add(file)
                }
            }
        return files
    }

    private fun stringToNewFile(value: String): File {
        val output = fileForPath(value)

        if (output.exists()) {
            if (output.isDirectory) {
                throw DriverException("$output is a directory")
            }
            val deleted = output.delete()
            if (!deleted) {
                throw DriverException("Could not delete previous version of $output")
            }
        } else if (output.parentFile != null && !output.parentFile.exists()) {
            val ok = output.parentFile.mkdirs()
            if (!ok) {
                throw DriverException("Could not create ${output.parentFile}")
            }
        }

        return output
    }

    private fun stringToNewOrExistingDir(value: String): File {
        val dir = fileForPath(value)
        if (!dir.isDirectory) {
            val ok = dir.mkdirs()
            if (!ok) {
                throw DriverException("Could not create $dir")
            }
        }
        return dir
    }

    private fun stringToNewDir(value: String): File {
        val output = fileForPath(value)
        val ok =
            if (output.exists()) {
                if (output.isDirectory) {
                    output.deleteRecursively()
                }
                if (output.exists()) {
                    true
                } else {
                    output.mkdir()
                }
            } else {
                output.mkdirs()
            }
        if (!ok) {
            throw DriverException("Could not create $output")
        }

        return output
    }

    private fun fileForPath(path: String): File {
        // java.io.File doesn't automatically handle ~/ -> home directory expansion.
        // This isn't necessary when metalava is run via the command line driver
        // (since shells will perform this expansion) but when metalava is run
        // directly, not from a shell.
        if (path.startsWith("~/")) {
            val home = System.getProperty("user.home") ?: return File(path)
            return File(home + path.substring(1))
        } else if (path.startsWith("@")) {
            return File("@" + File(path.substring(1)).absolutePath)
        }

        return File(path).absoluteFile
    }

    private fun getUsage(includeHeader: Boolean = true, colorize: Boolean = color): String {
        val usage = StringWriter()
        val printWriter = PrintWriter(usage)
        usage(printWriter, includeHeader, colorize)
        return usage.toString()
    }

    private fun usage(out: PrintWriter, includeHeader: Boolean = true, colorize: Boolean = color) {
        if (includeHeader) {
            out.println(wrap(HELP_PROLOGUE, MAX_LINE_WIDTH, ""))
        }

        if (colorize) {
            out.println("Usage: ${colorized(PROGRAM_NAME, TerminalColor.BLUE)} <flags>")
        } else {
            out.println("Usage: $PROGRAM_NAME <flags>")
        }

        val args = arrayOf(
            "", "\nGeneral:",
            ARG_HELP, "This message.",
            ARG_VERSION, "Show the version of $PROGRAM_NAME.",
            ARG_QUIET, "Only include vital output",
            ARG_VERBOSE, "Include extra diagnostic output",
            ARG_COLOR, "Attempt to colorize the output (defaults to true if \$TERM is xterm)",
            ARG_NO_COLOR, "Do not attempt to colorize the output",
            ARG_NO_DOCS, "Cancel any other documentation flags supplied to $PROGRAM_NAME. This is here " +
                "to make it easier customize build system tasks.",
            ARG_UPDATE_API, "Cancel any other \"action\" flags other than generating signature files. This is here " +
                "to make it easier customize build system tasks, particularly for the \"make update-api\" task.",

            "", "\nAPI sources:",
            "$ARG_SOURCE_FILES <files>", "A comma separated list of source files to be parsed. Can also be " +
                "@ followed by a path to a text file containing paths to the full set of files to parse.",

            "$ARG_SOURCE_PATH <paths>", "One or more directories (separated by `${File.pathSeparator}`) " +
                "containing source files (within a package hierarchy)",

            "$ARG_CLASS_PATH <paths>", "One or more directories or jars (separated by " +
                "`${File.pathSeparator}`) containing classes that should be on the classpath when parsing the " +
                "source files",

            "$ARG_MERGE_ANNOTATIONS <file>", "An external annotations file to merge and overlay " +
                "the sources, or a directory of such files. Formats supported are: IntelliJ's " +
                "external annotations database format, .jar or .zip files containing those, " +
                "Android signature files, and Java stub files.",

            "$ARG_INPUT_API_JAR <file>", "A .jar file to read APIs from directly",

            "$ARG_MANIFEST <file>", "A manifest file, used to for check permissions to cross check APIs",

            "$ARG_HIDE_PACKAGE <package>", "Remove the given packages from the API even if they have not been " +
                "marked with @hide",

            "$ARG_SHOW_ANNOTATION <annotation class>", "Include the given annotation in the API analysis",
            ARG_SHOW_UNANNOTATED, "Include un-annotated public APIs in the signature file as well",
            "$ARG_JAVA_SOURCE <level>", "Sets the source level for Java source files; default is 1.8.",

            "", "\nDocumentation:",
            ARG_PUBLIC, "Only include elements that are public",
            ARG_PROTECTED, "Only include elements that are public or protected",
            ARG_PACKAGE, "Only include elements that are public, protected or package protected",
            ARG_PRIVATE, "Include all elements except those that are marked hidden",
            ARG_HIDDEN, "Include all elements, including hidden",

            "", "\nExtracting Signature Files:",
            // TODO: Document --show-annotation!
            "$ARG_API <file>", "Generate a signature descriptor file",
            "$ARG_PRIVATE_API <file>", "Generate a signature descriptor file listing the exact private APIs",
            "$ARG_DEX_API <file>", "Generate a DEX signature descriptor file listing the APIs",
            "$ARG_PRIVATE_DEX_API <file>", "Generate a DEX signature descriptor file listing the exact private APIs",
            "$ARG_DEX_API_MAPPING <file>", "Generate a DEX signature descriptor along with file and line numbers",
            "$ARG_REMOVED_API <file>", "Generate a signature descriptor file for APIs that have been removed",
            "$ARG_OUTPUT_KOTLIN_NULLS[=yes|no]", "Controls whether nullness annotations should be formatted as " +
                "in Kotlin (with \"?\" for nullable types, \"\" for non nullable types, and \"!\" for unknown. " +
                "The default is yes.",
            "$ARG_OUTPUT_DEFAULT_VALUES[=yes|no]", "Controls whether default values should be included in " +
                "signature files. The default is yes.",
            "$ARGS_COMPAT_OUTPUT=[yes|no]", "Controls whether to keep signature files compatible with the " +
                "historical format (with its various quirks) or to generate the new format (which will also include " +
                "annotations that are part of the API, etc.)",
            "$ARG_OMIT_COMMON_PACKAGES[=yes|no]", "Skip common package prefixes like java.lang.* and " +
                "kotlin.* in signature files, along with packages for well known annotations like @Nullable and " +
                "@NonNull.",
            "$ARG_INCLUDE_SIG_VERSION[=yes|no]", "Whether the signature files should include a comment listing " +
                "the format version of the signature file.",

            "$ARG_PROGUARD <file>", "Write a ProGuard keep file for the API",
            "$ARG_SDK_VALUES <dir>", "Write SDK values files to the given directory",

            "", "\nGenerating Stubs:",
            "$ARG_STUBS <dir>", "Generate stub source files for the API",
            "$ARG_DOC_STUBS <dir>", "Generate documentation stub source files for the API. Documentation stub " +
                "files are similar to regular stub files, but there are some differences. For example, in " +
                "the stub files, we'll use special annotations like @RecentlyNonNull instead of @NonNull to " +
                "indicate that an element is recently marked as non null, whereas in the documentation stubs we'll " +
                "just list this as @NonNull. Another difference is that @doconly elements are included in " +
                "documentation stubs, but not regular stubs, etc.",
            ARG_EXCLUDE_ANNOTATIONS, "Exclude annotations such as @Nullable from the stub files",
            "$ARG_STUBS_SOURCE_LIST <file>", "Write the list of generated stub files into the given source " +
                "list file. If generating documentation stubs and you haven't also specified " +
                "$ARG_DOC_STUBS_SOURCE_LIST, this list will refer to the documentation stubs; " +
                "otherwise it's the non-documentation stubs.",
            "$ARG_DOC_STUBS_SOURCE_LIST <file>", "Write the list of generated doc stub files into the given source " +
                "list file",
            "$ARG_REGISTER_ARTIFACT <api-file> <id>", "Registers the given id for the packages found in " +
                "the given signature file. $PROGRAM_NAME will inject an @artifactId <id> tag into every top " +
                "level stub class in that API.",

            "", "\nDiffs and Checks:",
            "$ARG_INPUT_KOTLIN_NULLS[=yes|no]", "Whether the signature file being read should be " +
                "interpreted as having encoded its types using Kotlin style types: a suffix of \"?\" for nullable " +
                "types, no suffix for non nullable types, and \"!\" for unknown. The default is no.",
            "$ARG_CHECK_COMPATIBILITY:type:state <file>", "Check compatibility. Type is one of 'api' " +
                "and 'removed', which checks either the public api or the removed api. State is one of " +
                "'current' and 'released', to check either the currently in development API or the last publicly " +
                "released API, respectively. Different compatibility checks apply in the two scenarios. " +
                "For example, to check the code base against the current public API, use " +
                "$ARG_CHECK_COMPATIBILITY:api:current.",
            ARG_CHECK_KOTLIN_INTEROP, "Check API intended to be used from both Kotlin and Java for interoperability " +
                "issues",
            "$ARG_MIGRATE_NULLNESS <api file>", "Compare nullness information with the previous stable API " +
                "and mark newly annotated APIs as under migration.",
            ARG_WARNINGS_AS_ERRORS, "Promote all warnings to errors",
            ARG_LINTS_AS_ERRORS, "Promote all API lint warnings to errors",
            "$ARG_ERROR <id>", "Report issues of the given id as errors",
            "$ARG_WARNING <id>", "Report issues of the given id as warnings",
            "$ARG_LINT <id>", "Report issues of the given id as having lint-severity",
            "$ARG_HIDE <id>", "Hide/skip issues of the given id",

            "", "\nJDiff:",
            "$ARG_XML_API <file>", "Like $ARG_API, but emits the API in the JDiff XML format instead",
            "$ARG_CONVERT_TO_JDIFF <sig> <xml>", "Reads in the given signature file, and writes it out " +
                "in the JDiff XML format. Can be specified multiple times.",

            "", "\nStatistics:",
            ARG_ANNOTATION_COVERAGE_STATS, "Whether $PROGRAM_NAME should emit coverage statistics for " +
                "annotations, listing the percentage of the API that has been annotated with nullness information.",

            "$ARG_ANNOTATION_COVERAGE_OF <paths>", "One or more jars (separated by `${File.pathSeparator}`) " +
                "containing existing apps that we want to measure annotation coverage statistics for. The set of " +
                "API usages in those apps are counted up and the most frequently used APIs that are missing " +
                "annotation metadata are listed in descending order.",

            ARG_SKIP_JAVA_IN_COVERAGE_REPORT, "In the coverage annotation report, skip java.** and kotlin.** to " +
                "narrow the focus down to the Android framework APIs.",

            "$ARG_WRITE_CLASS_COVERAGE_TO <path>", "Specifies a file to write the annotation " +
                "coverage report for classes to.",
            "$ARG_WRITE_MEMBER_COVERAGE_TO <path>", "Specifies a file to write the annotation " +
                "coverage report for members to.",

            "", "\nExtracting Annotations:",
            "$ARG_EXTRACT_ANNOTATIONS <zipfile>", "Extracts source annotations from the source files and writes " +
                "them into the given zip file",
            "$ARG_INCLUDE_ANNOTATION_CLASSES <dir>", "Copies the given stub annotation source files into the " +
                "generated stub sources; <dir> is typically $PROGRAM_NAME/stub-annotations/src/main/java/.",
            "$ARG_REWRITE_ANNOTATIONS <dir/jar>", "For a bytecode folder or output jar, rewrites the " +
                "androidx annotations to be package private",
            "$ARG_COPY_ANNOTATIONS <source> <dest>", "For a source folder full of annotation " +
                "sources, generates corresponding package private versions of the same annotations.",
            ARG_INCLUDE_SOURCE_RETENTION, "If true, include source-retention annotations in the stub files. Does " +
                "not apply to signature files. Source retention annotations are extracted into the external " +
                "annotations files instead.",
            "", "\nInjecting API Levels:",
            "$ARG_APPLY_API_LEVELS <api-versions.xml>", "Reads an XML file containing API level descriptions " +
                "and merges the information into the documentation",

            "", "\nExtracting API Levels:",
            "$ARG_GENERATE_API_LEVELS <xmlfile>",
            "Reads android.jar SDK files and generates an XML file recording " +
                "the API level for each class, method and field",
            "$ARG_ANDROID_JAR_PATTERN <pattern>", "Patterns to use to locate Android JAR files. The default " +
                "is \$ANDROID_HOME/platforms/android-%/android.jar.",
            ARG_CURRENT_VERSION, "Sets the current API level of the current source code",
            ARG_CURRENT_CODENAME, "Sets the code name for the current source code",
            ARG_CURRENT_JAR, "Points to the current API jar, if any",

            "", "\nEnvironment Variables:",
            ENV_VAR_METALAVA_DUMP_ARGV, "Set to true to have metalava emit all the arguments it was invoked with. " +
                "Helpful when debugging or reproducing under a debugger what the build system is doing.",
            ENV_VAR_METALAVA_PREPEND_ARGS, "One or more arguments (concatenated by space) to insert into the " +
                "command line, before the documentation flags.",
            ENV_VAR_METALAVA_APPEND_ARGS, "One or more arguments (concatenated by space) to append to the " +
                "end of the command line, after the generate documentation flags."
        )

        var argWidth = 0
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            argWidth = Math.max(argWidth, arg.length)
            i += 2
        }
        argWidth += 2
        val sb = StringBuilder(20)
        for (indent in 0 until argWidth) {
            sb.append(' ')
        }
        val indent = sb.toString()
        val formatString = "%1$-" + argWidth + "s%2\$s"

        i = 0
        while (i < args.size) {
            val arg = args[i]
            val description = args[i + 1]
            if (arg.isEmpty()) {
                if (colorize) {
                    out.println(colorized(description, TerminalColor.YELLOW))
                } else {
                    out.println(description)
                }
            } else {
                if (colorize) {
                    val colorArg = bold(arg)
                    val invisibleChars = colorArg.length - arg.length
                    // +invisibleChars: the extra chars in the above are counted but don't contribute to width
                    // so allow more space
                    val colorFormatString = "%1$-" + (argWidth + invisibleChars) + "s%2\$s"

                    out.print(
                        wrap(
                            String.format(colorFormatString, colorArg, description),
                            MAX_LINE_WIDTH + invisibleChars, MAX_LINE_WIDTH, indent
                        )
                    )
                } else {
                    out.print(
                        wrap(
                            String.format(formatString, arg, description),
                            MAX_LINE_WIDTH, indent
                        )
                    )
                }
            }
            i += 2
        }
    }

    companion object {
        /** Whether we should use [Compatibility] mode */
        fun useCompatMode(args: Array<String>): Boolean {
            return COMPAT_MODE_BY_DEFAULT && !args.contains("$ARGS_COMPAT_OUTPUT=no")
        }
    }
}
