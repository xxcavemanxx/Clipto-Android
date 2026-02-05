package clipto.store.filter

import androidx.fragment.app.FragmentActivity
import clipto.common.misc.AndroidUtils
import clipto.config.IAppConfig
import clipto.domain.Filter
import clipto.extensions.createSnippetKit
import clipto.extensions.createTag
import clipto.presentation.filter.details.FilterDetailsFragment
import clipto.store.StoreObject
import clipto.store.StoreState
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class FilterDetailsState @Inject constructor(
    appConfig: IAppConfig,
    val filterState: FilterState
) : StoreState(appConfig) {

    val requestOpenFilter by lazy {
        StoreObject<OpenFilterRequest>(
            id = "request_open_filter",
            onChanged = { _, _ ->
                filterState.requestUpdateFilter.clearValue()
            },
            liveDataStrategy = StoreObject.LiveDataStrategy.SINGLE_CONSUMER
        )
    }

    fun requestNewTag(requestApply: Boolean = false, onCreated: ((filter: Filter) -> Unit)? = null) = requestOpenFilter(
        OpenFilterRequest(
            filter = Filter.createTag(),
            requestApply = requestApply,
            onCreated = onCreated
        )
    )

    fun requestNewSnippetKit(requestApply: Boolean = false, onCreated: ((filter: Filter) -> Unit)? = null) = requestOpenFilter(
        OpenFilterRequest(
            filter = Filter.createSnippetKit(),
            requestApply = requestApply,
            onCreated = onCreated
        )
    )

    fun requestOpenFilter(filter: Filter) = requestOpenFilter(OpenFilterRequest(filter = filter))

    fun requestOpenFilter(request: OpenFilterRequest) = requestOpenFilter.setValue(request)

    fun bind(activity: FragmentActivity) {
        requestOpenFilter.getLiveData().observe(activity) {
            FilterDetailsFragment.show(activity)
        }
    }

    data class OpenFilterRequest(
        val id: Int = AndroidUtils.nextId(),
        val requestApply: Boolean = false,
        val onCreated: ((filter: Filter) -> Unit)? = null,
        val filter: Filter
    )
}