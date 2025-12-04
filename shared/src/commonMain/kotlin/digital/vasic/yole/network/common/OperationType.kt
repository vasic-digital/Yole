package digital.vasic.yole.network.common

/**
 * Types of network operations.
 */
enum class OperationType {
    UPLOAD,
    DOWNLOAD,
    DELETE,
    CREATE_FOLDER,
    RENAME,
    MOVE,
    COPY,
    SYNC,
    SEARCH
}