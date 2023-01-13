
sealed class AppUpdaterState {
    object None: AppUpdaterState()
    object Checking: AppUpdaterState()
    data class Available(val url: URL, val lastVersionInformation: LastAvailableVersion): AppUpdaterState()
    data class Downloading(val totalBytes: Long, val bytesDownloaded: Long): AppUpdaterState()
    data class ReadyToInstall(val file: File, val lastVersionInformation: LastAvailableVersion): AppUpdaterState()
}
