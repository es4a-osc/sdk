package simple.library.wsd.viewpager;

/* Simple基础类 */
import simple.runtime.components.组件容器;
import simple.runtime.components.可视组件;
import simple.runtime.components.布局;
import simple.runtime.components.组件;
import simple.runtime.components.impl.android.视图组件;
import simple.runtime.components.impl.android.视图组件容器;
import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;

/* Android类 */ 
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/* Android扩展类库V4类 */ 
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

/* Java类 */
import java.util.ArrayList;

/**
 * 滑动页面框组件的实现
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 滑动页面框Impl extends 视图组件 implements 滑动页面框, 视图组件容器 {

	// 是否开启切换特效
	private boolean switchEffects;
	// 页面视图列表
	private ArrayList<View> pageViews;
	// 适配器
	private PagerAdapter adapter;
	// 是否禁止切换
	private boolean noScroll;

	/**
	 * 创建新组件
	 *
	 * @param container 容纳组件的容器（对于不可见的组件必须是窗口，不可为{@code null}）
	 */
	public 滑动页面框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		pageViews = new ArrayList<View>();

		// PagerAdapter https://developer.android.google.cn/reference/android/support/v4/view/PagerAdapter
		adapter = new PagerAdapter() {
			@Override
			public int getCount() {
				return pageViews.size();
			}
			
			@Override
			public boolean isViewFromObject(View view, Object object) {
				return view == object;
			}
			
			@Override
			public int getItemPosition(Object object) {
				return -2;
			}
			
			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
				container.removeView((View) object);
			}
			
			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				container.addView((View) pageViews.get(position));
				return pageViews.get(position);
			}
			
			@Override
			public Parcelable saveState() {
				return null;
			}
		};

		// ViewPager https://developer.android.google.cn/reference/android/support/v4/view/ViewPager
		ViewPager view = new ViewPager(MainActivity.getContext()) {
			@Override
			public boolean onTouchEvent(MotionEvent ev) {
				if (noScroll) {
					return false;
				}
				return super.onTouchEvent(ev);
			}
		
			@Override
			public boolean onInterceptTouchEvent(MotionEvent ev) {
				if (noScroll) {
					return false;
				}
				return super.onInterceptTouchEvent(ev);
			}
		};

		// 页面改变监听器
		view.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int state) {
				滑动状态改变(state);
			}
		
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				页面被滑动(position, positionOffset, positionOffsetPixels);
			}
		
			@Override
			public void onPageSelected(int position) {
				页面被选择(position);
			}
		});
		view.setFocusable(true);
		view.setAdapter(adapter);
		return view;
	}

	@Override
	public ViewGroup getLayoutManager() {
		return (ViewGroup) getView();
	}

	@Override
	public void addComponent(组件 component) {
		// 把可视组件接口转为实现接口视图组件，并取视图组件的视图
		View view = ((视图组件) component).getView();
		addPageView(view);
	}

	/**
	 * 页面由适配器延迟挂载，须先建立布局参数，供 Simple 初始化宽高属性使用。
	 */
	private void addPageView(View view) {
		// 页面尚未挂载到 ViewPager，布局参数可能为空。
		// 必须提前建立参数，否则 Simple 初始化宽高时会发生空指针异常。
		ViewGroup.LayoutParams current = view.getLayoutParams();
		// 已有 ViewPager 参数直接沿用，避免丢失其状态。
		if (!(current instanceof ViewPager.LayoutParams)) {
			// 使用 ViewPager 专用参数，默认宽高均为匹配父级。
			ViewPager.LayoutParams params = new ViewPager.LayoutParams();
			if (current != null) {
				// 从其他容器移入的页面保留已有宽高。
				params.width = current.width;
				params.height = current.height;
			}
			view.setLayoutParams(params);
		}
		// 先准备布局参数，再登记页面并通知适配器。
		pageViews.add(view);
		adapter.notifyDataSetChanged();
	}

	@Override
	public 布局 getLayout() {
		return null;
	}

	/* 滑动页面框 实现 */

	@Override
	public void 页面被滑动(int position, float positionOffset, int positionOffsetPixels) {
		EventDispatcher.dispatchEvent(this, "页面被滑动", position, positionOffset, positionOffsetPixels);
	}
	
	@Override
	public void 滑动状态改变(int state) {
		EventDispatcher.dispatchEvent(this, "滑动状态改变", state);
	}
	
	@Override
	public void 页面被选择(int position) {
		EventDispatcher.dispatchEvent(this, "页面被选择", position);
	}

	@Override
	public boolean 禁止滑动() {
		return noScroll;
	}

	@Override
	public void 禁止滑动(boolean noScroll) {
		this.noScroll = noScroll;
	}

	@Override
	public boolean 切换特效() {
		return switchEffects;
	}

	@Override
	public void 切换特效(boolean isOpen) {
		switchEffects = isOpen;
	}

	@Override
	public int 现行页面() {
		if (pageViews.size() == 0) {
			return -1;
		}
		ViewPager view = (ViewPager) getView();
		return view.getCurrentItem();
	}

	@Override
	public void 现行页面(int position) {
		ViewPager view = (ViewPager) getView();
		view.setCurrentItem(position, switchEffects);
	}

	@Override
	public void 添加页面(可视组件 visibleComponent) {
		try {
			// 把可视组件接口转为实现接口视图组件
			视图组件 viewComponent = (视图组件) visibleComponent;

			// 将视图从父级布局中移除
			View view = viewComponent.getView();
			ViewGroup group = (ViewGroup) view.getParent();
			if (group != null) {
				group.removeView(view);
			}
			
			// 将欲添加组件添加到页面视图
			addPageView(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void 删除页面(int position) {
		if (pageViews.size() > position) {
			pageViews.remove(position);
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void 清空页面() {
		pageViews.clear();
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 取页面数() {
		return pageViews.size();
	}
}
