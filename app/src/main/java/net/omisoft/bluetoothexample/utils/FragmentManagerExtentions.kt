package net.omisoft.bluetoothexample.utils

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager


fun FragmentManager.replaceFragment(@IdRes container: Int,
                                            fragment: Fragment,
                                            fragmentTag: String = fragment.javaClass.name,
                                            addToBackStack: Boolean = true) {
    popBackStack()
    showFragment(container, fragment, fragmentTag, addToBackStack)
}

fun FragmentManager.showFragmentAsRoot(@IdRes container: Int,
                                               fragment: Fragment,
                                               fragmentTag: String = fragment.javaClass.name,
                                               addToBackStack: Boolean = true) {
    clearBackStack()
    showFragment(container, fragment, fragmentTag, addToBackStack)
}

private fun FragmentManager.clearBackStack() {
    val fragmentsCount = backStackEntryCount
    for (i in 0 until fragmentsCount) {
        popBackStack()
    }
}

fun FragmentManager.showFragment(@IdRes container: Int,
                                         fragment: Fragment,
                                         fragmentTag: String = fragment.javaClass.name,
                                         addToBackStack: Boolean = true) {
    beginTransaction()
            .replace(container, fragment, fragmentTag)
            .apply {
                if (addToBackStack) addToBackStack(fragmentTag)
            }
            .commitAllowingStateLoss()
}