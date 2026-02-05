package clipto.common.presentation.state;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.Arrays;
import java.util.List;

public final class ViewPagerState {

    private List<FragmentPageProvider> fragmentPages;
    private FragmentManager fragmentManager;
    private List<PageProvider> viewPages;
    private int offscreenPageLimit;
    private TabLayout tabLayout;
    private Object selectedPage;
    private PagerAdapter adapter;

    public ViewPager apply(ViewPager viewPager) {
        if (viewPages != null) {
            adapter = createPageProvider(viewPager);
        } else if (fragmentPages != null) {
            adapter = createFragmentPageProvider(viewPager);
        }
        viewPager.setAdapter(adapter);

        if (tabLayout != null) {
            tabLayout.setupWithViewPager(viewPager);
        }
        return viewPager;
    }

    private PagerAdapter createPageProvider(final ViewPager viewPager) {
        final List<PageProvider> pages = viewPages;
        if (offscreenPageLimit == 0) {
            offscreenPageLimit = pages.size();
        }
        viewPager.setOffscreenPageLimit(offscreenPageLimit);

        return new PagerAdapter() {

            private PageProvider selectedPageProvider;

            @Override
            public int getCount() {
                return pages.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                if (container.getChildCount() >= position + 1) {
                    return container.getChildAt(position);
                } else {
                    for (int i = container.getChildCount(); i < position; i++) {
                        PageProvider pageProvider = pages.get(i);
                        View page = pageProvider.getPage(container.getContext());
                        container.addView(page, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    }
                }
                PageProvider pageProvider = pages.get(position);
                if (this.selectedPageProvider == pageProvider) {
                    selectedPageProvider = null;
                }
                View page = pageProvider.getPage(container.getContext());
                container.addView(page, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                return page;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                PageProvider pageProvider = pages.get(position);
                View page = (View) object;
                pageProvider.destroyPage(page);
                container.removeView(page);
            }

            @Override
            public void setPrimaryItem(ViewGroup container, int position, Object object) {
                PageProvider pageProvider = pages.get(position);
                if (selectedPageProvider != pageProvider) {
                    selectedPageProvider = pageProvider;
                    View page = (View) object;
                    pageProvider.selectPage(page);
                    selectedPage = page;
                }
            }

            @Override
            public CharSequence getPageTitle(int position) {
                PageProvider pageProvider = pages.get(position);
                return pageProvider.getTitle(viewPager.getContext());
            }

        };
    }

    private PagerAdapter createFragmentPageProvider(final ViewPager viewPager) {
        final List<FragmentPageProvider> pages = fragmentPages;
        if (offscreenPageLimit == 0) {
            offscreenPageLimit = fragmentPages.size();
        }
        viewPager.setOffscreenPageLimit(offscreenPageLimit);
        return new FragmentStatePagerAdapter(fragmentManager) {

            private FragmentPageProvider selectedPageProvider;

            @Override
            public int getCount() {
                return pages.size();
            }

            @Override
            public Fragment getItem(int i) {
                return pages.get(i).getPageFromCache(viewPager.getContext());
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                super.destroyItem(container, position, object);
                FragmentPageProvider pageProvider = pages.get(position);
                Fragment page = (Fragment) object;
                pageProvider.destroyPage(page);
                pageProvider.deletePageFromCache();
            }

            @Override
            public void setPrimaryItem(ViewGroup container, int position, Object object) {
                super.setPrimaryItem(container, position, object);
                FragmentPageProvider pageProvider = pages.get(position);
                if (selectedPageProvider != pageProvider) {
                    if (selectedPageProvider != null) {
                        Fragment prevPage = selectedPageProvider.getPageFromCache(container.getContext());
                        selectedPageProvider.unselectPage(prevPage);
                        prevPage.onPause();
                    }
                    Fragment page = pageProvider.getPageFromCache(container.getContext());
                    if (page.getActivity() != null) {
                        selectedPageProvider = pageProvider;
                        selectedPage = page;
                        pageProvider.getPageFromCache(container.getContext()).onResume();
                        pageProvider.selectPage(page);
                    }
                }
            }

            @Override
            public CharSequence getPageTitle(int position) {
                FragmentPageProvider pageProvider = pages.get(position);
                return pageProvider.getTitle(viewPager.getContext());
            }
        };
    }

    public Object getSelectedPage() {
        return selectedPage;
    }

    public ViewPagerState withOffscreenPageLimit(int offscreenPageLimit) {
        this.offscreenPageLimit = offscreenPageLimit;
        return this;
    }

    public ViewPagerState withTabLayout(TabLayout tabLayout) {
        this.tabLayout = tabLayout;
        return this;
    }

    public ViewPagerState withPages(PageProvider... pages) {
        return withPages(Arrays.asList(pages));
    }

    public ViewPagerState withPages(final List<PageProvider> pages) {
        this.viewPages = pages;
        return this;
    }

    public ViewPagerState withPages(FragmentManager fm, FragmentPageProvider... pages) {
        return withPages(fm, Arrays.asList(pages));
    }

    public ViewPagerState withPages(FragmentManager fm, List<FragmentPageProvider> pages) {
        this.fragmentPages = pages;
        this.fragmentManager = fm;
        return this;
    }

    public PagerAdapter getAdapter() {
        return adapter;
    }

    public static abstract class PageProvider<V extends View> {

        protected abstract CharSequence getTitle(Context context);

        protected abstract V getPage(Context context);

        protected void destroyPage(V page) {

        }

        protected void selectPage(V page) {

        }

    }

    public static abstract class FragmentPageProvider<V extends Fragment> {

        private V fragment;

        private V getPageFromCache(Context context) {
            if (fragment == null) {
                fragment = getPage(context);
            }
            return fragment;
        }

        private void deletePageFromCache() {
            fragment = null;
        }

        protected abstract CharSequence getTitle(Context context);

        protected abstract V getPage(Context context);

        protected void destroyPage(V page) {

        }

        protected void selectPage(V page) {
        }

        protected void unselectPage(V page) {

        }

    }

}