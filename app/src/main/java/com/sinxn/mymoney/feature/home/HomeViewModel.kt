package com.sinxn.mymoney.feature.home

import androidx.lifecycle.ViewModel
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    moneyDao: MoneyDao
) : ViewModel() {
    val wallets = moneyDao.getWallets()
}
