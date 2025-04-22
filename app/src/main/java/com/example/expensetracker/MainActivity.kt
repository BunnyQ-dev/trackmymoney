package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.expensetracker.adapters.ViewPagerAdapter
import com.example.expensetracker.databinding.ActivityMainBinding
import com.example.expensetracker.databinding.DialogSelectCurrencyBinding
import com.example.expensetracker.fragments.ExpenseChartFragment
import com.example.expensetracker.fragments.ExpenseFragment
import com.example.expensetracker.fragments.GoalsFragment
import com.example.expensetracker.fragments.IncomeChartFragment
import com.example.expensetracker.fragments.IncomeFragment
import com.example.expensetracker.fragments.TransactionsFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), 
    ExpenseFragment.ExpenseFragmentListener, 
    IncomeFragment.IncomeFragmentListener,
    GoalsFragment.GoalsFragmentListener {

    private lateinit var binding: ActivityMainBinding
    private var currentBalance: Double = 0.0
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private val fragments = mutableListOf<Fragment>()
    private val fragmentTitles = mutableListOf<String>()
    private lateinit var goalsFragment: GoalsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadBalance()
        
        // Оновлюємо символи валют при запуску
        binding.viewPager.post {
            updateCurrencySymbolsInFragments()
        }
    }
    
    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Setup ViewPager and TabLayout
        setupViewPager()
        
        // Setup Settings FAB
        binding.settingsFab.setOnClickListener {
            showSettingsMenu()
        }
        
        // Hide income dialog
        binding.incomeDialog.root.visibility = View.GONE
    }
    
    private fun setupViewPager() {
        // Initialize fragments
        val expenseFragment = ExpenseFragment().apply { setListener(this@MainActivity) }
        val incomeFragment = IncomeFragment().apply { setListener(this@MainActivity) }
        val expenseChartFragment = ExpenseChartFragment()
        val incomeChartFragment = IncomeChartFragment()
        val transactionsFragment = TransactionsFragment()
        goalsFragment = GoalsFragment()
        
        fragments.apply {
            add(expenseFragment)
            add(incomeFragment)
            add(expenseChartFragment)
            add(incomeChartFragment)
            add(transactionsFragment)
            add(goalsFragment)
        }
        
        fragmentTitles.apply {
            add(getString(R.string.title_expenses))
            add(getString(R.string.title_income))
            add(getString(R.string.title_expense_chart))
            add(getString(R.string.title_income_chart))
            add(getString(R.string.title_transactions))
            add(getString(R.string.my_goals))
        }
        
        viewPagerAdapter = ViewPagerAdapter(this, fragments, fragmentTitles)
        binding.viewPager.adapter = viewPagerAdapter
        
        // Setup TabLayout
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = fragmentTitles[position]
        }.attach()
    }
    
    private fun loadBalance() {
        lifecycleScope.launch {
            try {
                val token = TokenManager.getToken(this@MainActivity) ?: run {
                    Toast.makeText(this@MainActivity, getString(R.string.not_authorized), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                    return@launch
                }
                
                val authHeader = "Bearer $token"
                val response = ApiClient.apiService.getBalance(authHeader)
                
                if (response.isSuccessful) {
                    response.body()?.let { balanceResponse ->
                        currentBalance = balanceResponse.balance
                        updateBalanceUI()
                    }
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.failed_to_load_balance), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateBalanceUI() {
        val currencySymbol = CurrencyManager.getCurrencySymbol(this)
        binding.balanceAmount.text = String.format("%s%.2f", currencySymbol, currentBalance)
    }
    
    override fun onExpenseAdded() {
        // Update balance and charts after adding expense
        loadBalance()
        refreshCharts()
        refreshTransactions()
    }
    
    override fun onIncomeAdded() {
        // Update balance and charts after adding income
        loadBalance()
        refreshCharts()
        refreshTransactions()
    }
    
    private fun refreshCharts() {
        // Refresh charts if they are visible
        val expenseChartFragment = fragments.find { it is ExpenseChartFragment }
        val incomeChartFragment = fragments.find { it is IncomeChartFragment }
        val transactionsFragment = fragments.find { it is TransactionsFragment }
        
        if (expenseChartFragment != null && expenseChartFragment.isVisible) {
            (expenseChartFragment as ExpenseChartFragment).loadChartData()
        }
        
        if (incomeChartFragment != null && incomeChartFragment.isVisible) {
            (incomeChartFragment as IncomeChartFragment).loadChartData()
        }
        
        if (transactionsFragment != null && transactionsFragment.isVisible) {
            (transactionsFragment as TransactionsFragment).loadTransactions()
        }
    }
    
    private fun refreshTransactions() {
        val transactionsFragment = fragments.find { it is TransactionsFragment }
        transactionsFragment?.let {
            (it as TransactionsFragment).loadTransactions()
        }
    }
    
    private fun showSettingsMenu() {
        val popupMenu = PopupMenu(this, binding.settingsFab)
        popupMenu.inflate(R.menu.settings_menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_currency -> {
                    showCurrencyDialog()
                    true
                }
                R.id.action_change_password -> {
                    startActivity(Intent(this, ChangePasswordActivity::class.java))
                    true
                }
                R.id.action_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun showCurrencyDialog() {
        val dialogBinding = DialogSelectCurrencyBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val currentCurrencyIndex = CurrencyManager.getCurrencyIndex(this)
        val radioButtons = listOf(
            dialogBinding.uahRadio,
            dialogBinding.usdRadio,
            dialogBinding.eurRadio,
            dialogBinding.gbpRadio,
            dialogBinding.plnRadio
        )
        
        if (currentCurrencyIndex in radioButtons.indices) {
            radioButtons[currentCurrencyIndex].isChecked = true
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.saveCurrencyButton.setOnClickListener {

            val selectedIndex = when {
                dialogBinding.uahRadio.isChecked -> 0
                dialogBinding.usdRadio.isChecked -> 1
                dialogBinding.eurRadio.isChecked -> 2
                dialogBinding.gbpRadio.isChecked -> 3
                dialogBinding.plnRadio.isChecked -> 4
                else -> 0
            }
            

            CurrencyManager.setCurrencyIndex(this, selectedIndex)
            

            updateBalanceUI()
            

            updateCurrencySymbolsInFragments()
            
            Toast.makeText(this, R.string.currency_updated, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    

    private fun updateCurrencySymbolsInFragments() {

        fragments.forEach { fragment ->
            when (fragment) {
                is ExpenseFragment -> fragment.updateCurrencySymbol()
                is IncomeFragment -> fragment.updateCurrencySymbol()
                is GoalsFragment -> fragment.updateCurrencyDisplay()
            }
        }
    }
    
    private fun logout() {
        TokenManager.clearToken(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    override fun onBalanceChanged() {
        // Update balance after goal deposit/withdraw
        loadBalance()
    }
}