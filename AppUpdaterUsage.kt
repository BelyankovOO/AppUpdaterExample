private suspend fun checkForUpdates(context: Context) {
  if (_stateUpdater.value == AppUpdaterState.None) {
      setStateUpdater(AppUpdaterState.Checking)
      requestLastVersion(context)
  } else if (_stateUpdater.value is AppUpdaterState.Available || _stateUpdater.value is AppUpdaterState.ReadyToInstall) {
      setStateUpdater(_stateUpdater.value!!)
  }
}

private fun downloadFile(url: URL, lastAvailableVersion: LastAvailableVersion, context: Context, lifecycleOwner: LifecycleOwner) {
  val workManager = WorkManager.getInstance(context)
  val data = Data.Builder()

  data.apply {
      putString(FileParams.KEY_FILE_NAME, lastAvailableVersion.name)
      putString(FileParams.KEY_FILE_URL, url.toString())
      putLong(FileParams.KEY_FILE_SIZE, lastAvailableVersion.size ?: 0L)
  }

  val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresStorageNotLow(true)
      .setRequiresBatteryNotLow(true)
      .build()

  val fileDownloadWorker = OneTimeWorkRequestBuilder<FileDownloadWorker>()
      .setConstraints(constraints)
      .setInputData(data.build())
      .build()

  workManager.enqueueUniqueWork(
      "oneFileDownloadWork_${System.currentTimeMillis()}",
      ExistingWorkPolicy.KEEP,
      fileDownloadWorker
  )

  workManager.getWorkInfoByIdLiveData(fileDownloadWorker.id)
      .observe(lifecycleOwner){ info->
          info?.let {
              when (it.state) {
                  WorkInfo.State.SUCCEEDED -> {
                      val filePath = it.outputData.getString(FileParams.KEY_FILE_URI) ?: return@observe
                      setStateUpdater(AppUpdaterState.ReadyToInstall(File(filePath), lastAvailableVersion))
                  }
                  WorkInfo.State.FAILED -> {
                      setStateUpdater(AppUpdaterState.None)
                  }
                  WorkInfo.State.RUNNING -> {}
                  else -> {
                      setStateUpdater(AppUpdaterState.None)
                  }
              }
          }
      }
}

private suspend fun requestLastVersion(context: Context) = withContext(Dispatchers.IO) {
  val repositoryResource = repository.getLastAvailableVersion()

  if (repositoryResource.status == RepositoryStatus.SUCCESS) {
      repositoryResource.data?.android?.let { lastAvailableVersion ->
          val url = URL(lastAvailableVersion.path)
          val releaseCode = lastAvailableVersion.version.releaseCode() ?: return@withContext
          val appReleaseCode = BuildConfig.VERSION_NAME.releaseCode() ?: return@withContext
          val filePath = "${lastAvailableVersion.name}.apk"
          val file = File(context.getExternalFilesDir(null), filePath)

          if (releaseCode > appReleaseCode) {
              if (file.exists() && file.length() == lastAvailableVersion.size) {
                  setStateUpdater(AppUpdaterState.ReadyToInstall(file, lastAvailableVersion))
              } else {
                  setStateUpdater(AppUpdaterState.Available(url, lastAvailableVersion))
              }
          } else {
              setStateUpdater(AppUpdaterState.None)
          }
      }
  }
}
