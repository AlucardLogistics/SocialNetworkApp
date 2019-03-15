package com.logistics.alucard.socialnetwork.Utils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SectionsStatePagersAdapter extends FragmentStatePagerAdapter {

    private static final String TAG = "SectionsStatePagersAdap";

    private final List<Fragment> mFragmentList = new ArrayList<>();
    //store fragments and get their number or name
    private final HashMap<Fragment, Integer> mFragmets = new HashMap<>();
    private final HashMap<String, Integer> mFragmetsNumbers = new HashMap<>();
    private final HashMap<Integer, String> mFragmetsNames = new HashMap<>();

    public SectionsStatePagersAdapter(FragmentManager fm) {
        super(fm);
    }



    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public void addFragment(Fragment fragment, String fragmentName) {
        mFragmentList.add(fragment);
        mFragmets.put(fragment, mFragmentList.size()-1);
        mFragmetsNumbers.put(fragmentName, mFragmentList.size()-1);
        mFragmetsNames.put(mFragmentList.size()-1, fragmentName);
    }

    /*
    * returns the fragment with the name @param
    * @param is fragmentName
    * @return
    * */
    public Integer getFragmentNumber(String fragmentName) {
        if(mFragmetsNumbers.containsKey(fragmentName)) {
            return mFragmetsNumbers.get(fragmentName);
        } else {
            return null;
        }
    }

    /*
     * returns the fragment with the name @param
     * @param is fragmentName
     * @return
     * */
    public Integer getFragmentNumber(Fragment fragment) {
        if(mFragmetsNumbers.containsKey(fragment)) {
            return mFragmetsNumbers.get(fragment);
        } else {
            return null;
        }
    }

    /*
     * returns the fragment with the name @param
     * @param is fragmentNumber
     * @return
     * */
    public String getFragmentName(Integer fragmentNumber) {
        if(mFragmetsNames.containsKey(fragmentNumber)) {
            return mFragmetsNames.get(fragmentNumber);
        } else {
            return null;
        }
    }
}
