package clipto.presentation.blocks.domain

import com.wb.clipboard.databinding.ViewRatingbarBinding
import com.wb.clipboard.databinding.ViewFeedbackBinding
import com.wb.clipboard.databinding.BlockAboutBinding
import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import clipto.AppUtils
import clipto.analytics.Analytics
import clipto.common.extensions.safeIntent
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.showToast
import clipto.common.misc.GooglePlayUtils
import clipto.common.misc.IntentUtils
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.user.UserState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R

class AboutBlock<C>(
    private val withTitle: Boolean = true,
    private val userState: UserState
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_about

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is AboutBlock && withTitle == item.withTitle

    override fun onBind(context: C, block: View) {
        val binding = BlockAboutBinding.bind(block)
        val ctx = block.context
        binding.textCampaignTextView.setVisibleOrGone(withTitle)
        binding.rateButton.setOnClickListener {
            if (!userState.appConfig.canReportNegativeFeedback()) {
                val ratingBarView = View.inflate(ctx, R.layout.view_ratingbar, null)
                val ratingBarViewBinding = ViewRatingbarBinding.bind(ratingBarView)
                val ratingBar = ratingBarViewBinding.ratingBar
                val dialog = MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.settings_campaign_rate_question)
                    .setView(ratingBarView)
                    .create()
                ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
                    dialog.dismiss()
                    if (rating == 5f) {
                        ctx.showToast(R.string.settings_campaign_rate_feedback_toast)
                        ctx.safeIntent(GooglePlayUtils.rate(ctx))
                        Analytics.onRateFive()
                    } else {
                        onFeedback(rating, ctx)
                    }
                }
                dialog.show()
                Analytics.onReportNegativeFeedback()
            } else {
                Analytics.onRate()
                ctx.safeIntent(GooglePlayUtils.rate(ctx))
            }
        }
        binding.translateButton.setOnClickListener {
            Analytics.onTranslate()
            IntentUtils.open(ctx, userState.appConfig.getTranslateUrl())
        }
        binding.issueButton.setOnClickListener {
            Analytics.onIssue()
            IntentUtils.open(ctx, userState.appConfig.getGithubUrl())
        }
        binding.shareButton.setOnClickListener {
            userState.requestShareApp()
        }
        binding.emailButton.setOnClickListener {
            Analytics.onEmail()
            AppUtils.sendRequest()
        }
        binding.versionTextView.text = ctx.resources.getString(
            R.string.about_label_version,
            ctx.getString(R.string.app_name),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        binding.redditButton.setOnClickListener {
            Analytics.onReddit()
            IntentUtils.open(ctx, userState.appConfig.getRedditUrl())
        }
        binding.discordButton.setOnClickListener {
            Analytics.onDiscord()
            IntentUtils.open(ctx, userState.appConfig.getDiscordUrl())
        }
        binding.changelogButton.setOnClickListener {
            Analytics.onChangelog()
            IntentUtils.open(ctx, userState.appConfig.getChangelogUrl())
        }
        binding.privacyPolicy.setOnClickListener {
            Analytics.onPrivacyPolicy()
            IntentUtils.open(ctx, BuildConfig.privacyPolicyUrl)
        }
        binding.termsOfService.setOnClickListener {
            Analytics.onTermsOfService()
            IntentUtils.open(ctx, BuildConfig.tosUrl)
        }
        if (ctx is LifecycleOwner) {
            userState.user.getLiveData().observe(ctx) {
                binding.shareStatistics?.setVisibleOrGone(it.isAuthorized())
            }
            userState.invitations.getLiveData().observe(ctx) {
                binding.shareStatistics?.text = ctx.getString(R.string.about_label_campaign_share_description, it)
            }
        }
    }

    private fun onFeedback(rating: Float, ctx: Context) {
        val feedbackView = View.inflate(ctx, R.layout.view_feedback, null)
        val feedbackViewBinding = ViewFeedbackBinding.bind(feedbackView)
        val feedbackText = feedbackViewBinding.feedbackText
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.settings_campaign_rate_feedback_title)
            .setView(feedbackView)
            .setPositiveButton(R.string.button_send) { dialog, _ ->
                dialog.cancel()
                Analytics.onReportNegativeFeedback()
                AppUtils.sendRequest(feedbackText.text.toString(), "Rating: ${rating.toInt()}")
            }
            .setNegativeButton(R.string.menu_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }
}
