package com.adp.esi.digitech.ds.config.constants;

/**
 * Centralized API endpoint and error message constants.
 * Ensures maintainability and avoids magic strings in codebase.
 *
 * @author rhidau
 */
public final class ApiConstants {

    private ApiConstants() {}

    public static final String BASE_URL = "/api/v1/xsd";
    public static final String UPLOAD = "/upload";
    public static final String SELECT = "/select";
    public static final String SCHEMAS = "/schemas";
    public static final String SCHEMA_BY_ID = "/schemas/{id}";

    // Request parameter names
    public static final String SOURCE_KEY_PARAM = "sourceKey";
    public static final String FILE_PARAM = "file";
    public static final String BU_PARAM = "bu";
    public static final String PLATFORM_PARAM = "platform";
    public static final String DATA_CATEGORY_PARAM = "dataCategory";

    // Error codes/messages
    public static final String API_ERROR = "API_ERROR";
    public static final String FILE_EMPTY_ERROR = "Uploaded file is empty.";
    public static final String SCHEMA_NOT_FOUND = "Schema configuration not found.";
    public static final String DUPLICATE_SOURCE_KEY = "Configuration with the provided sourceKey already exists.";
}