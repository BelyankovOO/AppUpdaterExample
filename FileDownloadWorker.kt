object FileParams{
    const val KEY_FILE_URL = "key_file_url"
    const val KEY_FILE_NAME = "key_file_name"
    const val KEY_FILE_URI = "key_file_uri"
    const val KEY_FILE_SIZE = "key_file_size"
}

object NotificationConstants{
    const val CHANNEL_NAME = "download_file_worker_channel"
    const val CHANNEL_DESCRIPTION = "download_file_worker_description"
    const val CHANNEL_ID = "download_file_worker_channel"
    const val NOTIFICATION_ID = 1
}

class FileDownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val fileUrl = inputData.getString(FileParams.KEY_FILE_URL) ?: ""
        val fileName = inputData.getString(FileParams.KEY_FILE_NAME) ?: ""
        val fileSize = inputData.getLong(FileParams.KEY_FILE_SIZE, 0)

        if (fileName.isEmpty()
            || fileUrl.isEmpty()
            || fileSize == 0L
        ){
            return Result.failure()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = NotificationConstants.CHANNEL_NAME
            val description = NotificationConstants.CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationConstants.CHANNEL_ID,name,importance)
            channel.description = description
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context,NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_outline_file_download_24)
            .setContentTitle("Загрузка обновления...")
            .setOngoing(true)
            .setProgress(0,0,true)

        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIFICATION_ID,builder.build())

        val file = getSavedFile(
            context = context,
            URL(fileUrl),
            fileName,
            fileSize
        )

        NotificationManagerCompat.from(context).cancel(NotificationConstants.NOTIFICATION_ID)

        Log.d("Check", "${file.length()} $fileSize")

        return if (file.length() == fileSize) {
            Result.success(workDataOf(FileParams.KEY_FILE_URI to file.path.toString()))
        } else {
            Result.failure()
        }
    }

    private fun getSavedFile(
        context: Context,
        fileURL: URL,
        fileTitle: String,
        fileSize: Long
    ): File {
        val apkAppFile = File(context.getExternalFilesDir(null), "${fileTitle}.apk")

        if (apkAppFile.exists() && apkAppFile.length() == fileSize) {
            return apkAppFile
        }

        apkAppFile.delete()

        val connection = fileURL.openConnection()
        connection.getInputStream().use { inp ->
            BufferedInputStream(inp).use { bis ->
                FileOutputStream(apkAppFile).use { fos ->
                    val data = ByteArray(2048)
                    var count: Int
                    while (bis.read(data, 0, 2048).also { count = it } != -1) {
                        fos.write(data, 0, count)
                    }
                }
            }
        }

        return apkAppFile
    }

}
