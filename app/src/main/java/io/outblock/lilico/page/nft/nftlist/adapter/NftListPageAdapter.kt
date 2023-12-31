package io.outblock.lilico.page.nft.nftlist.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import io.outblock.lilico.page.nft.nftlist.NFTFragment
import io.outblock.lilico.page.nft.nftlist.NftGridFragment
import io.outblock.lilico.page.nft.nftlist.NftListFragment

class NftListPageAdapter(
    fragment: NFTFragment
) : FragmentStatePagerAdapter(fragment.childFragmentManager) {

    override fun getCount(): Int = 2

    override fun getItem(position: Int): Fragment {
        return when (position) {
            1 -> NftGridFragment.newInstance()
            else -> NftListFragment.newInstance()
        }
    }
}