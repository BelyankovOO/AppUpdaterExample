class InstallNewAppVersion(private val file: File, private val lastAvailableVersion: LastAvailableVersion) : BaseBindingBottomSheet<NewAppVersionFragmentBinding>(
    NewAppVersionFragmentBinding::inflate
) {
    private var buttonLayoutParams: ConstraintLayout.LayoutParams? = null

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(requireBinding()) {
            closeButton.setOnClickListener {
                dismiss()
            }
            sheetButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = FileProvider.getUriForFile(requireContext(), "com.example.crushpro.FileProvider", file)
                intent.setDataAndType(
                    uri,
                    "application/vnd.android.package-archive"
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    requireContext().startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    Log.e("TAG", "Error in opening the file!")
                }
            }
            sheetTitle.text = "Обновление загружено"
            sheetButton.text = "Установить"
            nameValue.text = lastAvailableVersion.name
            if (lastAvailableVersion.info != null) {
                descriptionTitle.visibility = View.VISIBLE
                descriptionValue.visibility = View.VISIBLE
                descriptionValue.text = lastAvailableVersion.info
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!(file.exists() && file.length() == lastAvailableVersion.size)) {
            dismissAllowingStateLoss()
        }
        //dialog?.window?.setWindowAnimations(-1)
    }
