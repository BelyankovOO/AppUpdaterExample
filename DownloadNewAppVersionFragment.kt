class DownloadNewAppVersion(private val fileURL: URL, private val lastAvailableVersion: LastAvailableVersion) : BaseBindingBottomSheet<NewAppVersionFragmentBinding>(
    NewAppVersionFragmentBinding::inflate
) {
    private val viewModel: ActivityViewModel by activityViewModels()
    private var buttonLayoutParams: ConstraintLayout.LayoutParams? = null

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(requireBinding()) {
            closeButton.setOnClickListener {
                dismiss()
            }
            sheetButton.setOnClickListener {
                viewModel.onTriggerEvent(ActivityListEvent.DownloadFile(
                    fileUrl = fileURL,
                    lastAvailableVersion = lastAvailableVersion,
                    context = requireActivity(),
                    lifecycleOwner = requireActivity()
                ))
                dismiss()
            }
            sheetTitle.text = "Доступна новая версия"
            nameValue.text = lastAvailableVersion.name
            if (lastAvailableVersion.size != null) {
                sizeTitle.visibility = View.VISIBLE
                sizeValue.visibility = View.VISIBLE
                sizeValue.text = this.root.resources.getString(
                    R.string.new_app_version_size_value,
                    lastAvailableVersion.size.toDouble() / (1024 * 1024)
                )
            }
            if (lastAvailableVersion.info != null) {
                descriptionTitle.visibility = View.VISIBLE
                descriptionValue.visibility = View.VISIBLE
                descriptionValue.text = lastAvailableVersion.info
            }
        }
    }

    override fun onStop() {
        super.onStop()
        //dialog?.window?.setWindowAnimations(-1)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            bottomSheetDialog.setCanceledOnTouchOutside(false)
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { bottomSheet ->
                val behaviour = BottomSheetBehavior.from(bottomSheet)
                with(requireBinding()) {
                    buttonLayoutParams =
                        sheetButtonLayout.layoutParams as ConstraintLayout.LayoutParams
                    behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                    val bottomSheetLayoutParams = bottomSheet.layoutParams
                    bottomSheetLayoutParams.height = activity?.windowManager?.getWindowHeight()!! * 99 / 100
                    val expandedHeight = bottomSheetLayoutParams.height
                    bottomSheet.layoutParams = bottomSheetLayoutParams
                    val peekHeight = expandedHeight * 40 / 100
                    behaviour.skipCollapsed = false
                    behaviour.peekHeight = peekHeight
                    behaviour.isHideable = true

                    val buttonHeight = sheetButtonLayout.height
                    val collapsedMargin = peekHeight - buttonHeight
                    buttonLayoutParams?.topMargin = collapsedMargin
                    sheetButtonLayout.layoutParams = buttonLayoutParams

                    behaviour.addBottomSheetCallback(object :
                        BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            val offset: Float?
                            if (slideOffset >= 0) {
                                offset =
                                    (expandedHeight - (buttonHeight + collapsedMargin)) * slideOffset + collapsedMargin
                                buttonLayoutParams?.topMargin = offset.toInt()
                            } else {
                                offset = collapsedMargin + peekHeight * slideOffset
                                if (offset > 0) {
                                    buttonLayoutParams?.topMargin = offset.toInt()
                                } else {
                                    buttonLayoutParams?.topMargin = -offset.toInt()
                                }
                            }
                            sheetButtonLayout.layoutParams = buttonLayoutParams
                        }
                    })
                }
            }
        }
        return dialog
    }
}
