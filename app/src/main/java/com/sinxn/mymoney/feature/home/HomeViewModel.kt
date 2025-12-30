package com.sinxn.mymoney.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class HomeViewModel @Inject constructor(
    moneyDao: MoneyDao
) : ViewModel() {
    val wallets = moneyDao.getWalletsWithBalance()
        .onEach { Log.d("HomeViewModel", "Wallets emitted: ${it.size}") }
}
