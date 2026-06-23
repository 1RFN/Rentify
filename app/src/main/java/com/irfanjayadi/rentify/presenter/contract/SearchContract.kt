package com.irfanjayadi.rentify.presenter.contract

import com.irfanjayadi.rentify.model.entity.Item

interface SearchContract {
    interface View {
        fun showLoading()
        fun hideLoading()
        fun showSearchResults(items: List<Item>)
        fun showEmptyState()
        fun showError(message: String)
    }

    interface Presenter {
        fun searchItems(keyword: String, category: String, sortBy: String, location: String = "")
    }
}