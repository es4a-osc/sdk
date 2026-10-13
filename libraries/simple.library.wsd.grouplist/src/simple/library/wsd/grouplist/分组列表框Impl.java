package simple.library.wsd.grouplist;

import simple.runtime.components.impl.android.视图组件;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 分组列表框组件的Android实现
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 分组列表框Impl extends 视图组件 implements 分组列表框 {

	/*
	ExpandableListView
	https://developer.android.com/reference/android/widget/ExpandableListView
	*/

	private ExpandableListView expandableListView;
	private boolean haveButton;
	private String backgroundImage;
	private int backgroundImage2;
	private ExpandableAdapter adapter;
	private List<Map<String, String>> groups;
	private List<List<Map<String, String>>> childlist;

	private int groupTitletextSize;
	private int groupTitletextColor;
	private int groupInfotextSize;
	private int groupInfotextColor;

	private int childTitletextSize;
	private int childTitletextColor;
	private int childInfotextSize;
	private int childInfotextColor;
	private int childButtontextSize;
	private int childButtontextColor;

	public 分组列表框Impl(组件容器 container) {
		super(container);

		haveButton = true;

		backgroundImage = "";
		backgroundImage2 = -1;

		groupTitletextSize = 14;
		groupTitletextColor = 颜色_黑;

		groupInfotextSize = 12;
		groupInfotextColor = 颜色_灰;

		childTitletextSize = 14;
		childTitletextColor = 颜色_黑;

		childInfotextSize = 12;
		childInfotextColor = 颜色_灰;

		childButtontextSize = 12;
		childButtontextColor = 颜色_黑;
	}

	/* 视图组件 相关方法 */

	@Override
	protected View createView() {
		expandableListView = new ExpandableListView(MainActivity.getContext());
		expandableListView.setFocusable(true);
		expandableListView.setCacheColorHint(0);

		groups = new ArrayList<>();
		childlist = new ArrayList<>();
		adapter = new ExpandableAdapter(MainActivity.getContext());

		expandableListView.setAdapter(adapter);
		expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				子项被单击(groupPosition, childPosition);
				return false;
			}
		});

		expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View childView, int flatPos, long id) {
				if (ExpandableListView.getPackedPositionType(id) == 1) {
					long packedPos = ((ExpandableListView) parent).getExpandableListPosition(flatPos);
					int groupPosition = ExpandableListView.getPackedPositionGroup(packedPos);
					int childPosition = ExpandableListView.getPackedPositionChild(packedPos);

					子项被长按(groupPosition, childPosition);
					return true;
				}

				return false;
			}
		});

		expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
			   分组被展开(groupPosition);
			}
		});

		expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
			@Override
			public void onGroupCollapse(int groupPosition) {
				分组被收起(groupPosition);
			}
		});

		expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				分组被单击(groupPosition);
				return false;
			}
		});
		return expandableListView;
	}

	/* 事件 */

	@Override
	public void 子项被单击(int groupId, int childId) {
		EventDispatcher.dispatchEvent(this, "子项被单击", groupId, childId);
	}

	@Override
	public void 子项被长按(int groupId, int childId) {
		EventDispatcher.dispatchEvent(this, "子项被长按", groupId, childId);
	}

	@Override
	public void 子项按钮被单击(int groupId, int childId) {
		EventDispatcher.dispatchEvent(this, "子项按钮被单击", groupId, childId);
	}

	@Override
	public void 分组被展开(int groupId) {
		EventDispatcher.dispatchEvent(this, "分组被展开", groupId);
	}

	@Override
	public void 分组被收起(int groupId) {
		EventDispatcher.dispatchEvent(this, "分组被收起", groupId);
	}

	@Override
	public void 分组被单击(int groupId) {
		EventDispatcher.dispatchEvent(this, "分组被单击", groupId);
	}

	/* 属性 */

	@Override
	public int 分组标题字体大小() {
		return groupTitletextSize;
	}

	@Override
	public void 分组标题字体大小(int size) {
		groupTitletextSize = size;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 分组标题字体颜色() {
		return groupTitletextColor;
	}

	@Override
	public void 分组标题字体颜色(int argb) {
		groupTitletextColor = argb;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 分组信息字体大小() {
		return groupInfotextSize;
	}

	@Override
	public void 分组信息字体大小(int size) {
		groupInfotextSize = size;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 分组信息字体颜色() {
		return groupInfotextColor;
	}

	@Override
	public void 分组信息字体颜色(int argb) {
		groupInfotextColor = argb;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 子项标题字体大小() {
		return childTitletextSize;
	}

	@Override
	public void 子项标题字体大小(int size) {
		childTitletextSize = size;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 子项标题字体颜色() {
		return childTitletextColor;
	}

	@Override
	public void 子项标题字体颜色(int argb) {
		childTitletextColor = argb;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 子项信息字体大小() {
		return childInfotextSize;
	}

	@Override
	public void 子项信息字体大小(int size) {
		childInfotextSize = size;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 子项信息字体颜色() {
		return childInfotextColor;
	}

	@Override
	public void 子项信息字体颜色(int argb) {
		childInfotextColor = argb;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 子项按钮字体大小() {
		return childButtontextSize;
	}

	@Override
	public void 子项按钮字体大小(int size) {
		childButtontextSize = size;
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 子项按钮字体颜色() {
		return childButtontextColor;
	}

	@Override
	public void 子项按钮字体颜色(int argb) {
		childButtontextColor = argb;
		adapter.notifyDataSetChanged();
	}

	@Override
	public String 背景图片() {
		return backgroundImage;
	}

	@Override
	public void 背景图片(String imagePath) {
		backgroundImage = imagePath;
		Drawable drawable = null;

		if (imagePath.length() > 0) {
			if (imagePath.startsWith("/")) {
				File f = new File(imagePath);

				if (f.exists()) {
					drawable = Drawable.createFromPath(imagePath);
				}
			} else {
				try {
					drawable = Drawable.createFromStream(MainActivity.getContext().getResources().getAssets().open(imagePath), imagePath);
				} catch (IOException e) {

				}
			}

			expandableListView.setBackgroundDrawable(drawable);
			expandableListView.invalidate();
		}
	}

	@Override
	public int 背景图片2() {
		return backgroundImage2;
	}

	@Override
	public void 背景图片2(int id) {
		backgroundImage2 = id;
		View view = getView();

		view.setBackgroundResource(id);
	}

	@Override
	public boolean 显示子项按钮() {
		return haveButton;
	}

	@Override
	public void 显示子项按钮(boolean value) {
		haveButton = value;
		adapter.notifyDataSetChanged();
	}

	/* 方法 */

	@Override
	public boolean 获取焦点() {
		return ViewUtil.requestFocus(getView());
	}

	@Override
	public void 清除焦点() {
		ViewUtil.clearFocus(getView());
	}

	@Override
	public int 添加分组(String title, String info) {
		Map<String, String> map = new HashMap<>();

		map.put("title", title);
		map.put("info", info);
		map.put("tag", "");
		groups.add(map);

		List<Map<String, String>> childItems = new ArrayList<>();

		childlist.add(childItems);
		adapter.notifyDataSetChanged();
		return groups.size() - 1;
	}

	@Override
	public int 添加分组(String title) {
		return 添加分组(title, "");
	}

	@Override
	public String 取分组标记(int position) {
		return groups.get(position).get("tag");
	}

	@Override
	public void 置分组标记(int position, String tag) {
		groups.get(position).put("tag", tag);
	}

	@Override
	public String 取分组标题(int groupId) {
		return groups.get(groupId).get("title");
	}

	@Override
	public void 置分组标题(int groupId, String title) {
		groups.get(groupId).put("title", title);
		adapter.notifyDataSetChanged();
	}

	@Override
	public String 取分组信息(int groupId) {
		return groups.get(groupId).get("info");
	}

	@Override
	public void 置分组信息(int groupId, String info) {
		groups.get(groupId).put("info", info);
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 取分组总数() {
		return groups.size();
	}

	@Override
	public void 展开分组(int groupId) {
		expandableListView.expandGroup(groupId);
	}

	@Override
	public void 收起分组(int groupId) {
		expandableListView.collapseGroup(groupId);
	}

	@Override
	public boolean 取分组状态(int groupId) {
		return expandableListView.isGroupExpanded(groupId);
	}

	@Override
	public void 选中分组(int groupPosition) {
		expandableListView.setSelectedGroup(groupPosition);
	}

	@Override
	public void 删除分组(int groupId) {
		childlist.remove(groupId);
		groups.remove(groupId);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void 清空所有数据() {
		groups.clear();
		childlist.clear();
		adapter.notifyDataSetChanged();
	}

	@Override
	public void 添加子项(int groupId, String image, String title, String info, String buttonImage, String buttonTitle) {
		Map<String, String> map = new HashMap<>();

		map.put("image", image);
		map.put("title", title);
		map.put("info", info);
		map.put("buttonImage", buttonImage);
		map.put("buttonTitle", buttonTitle);
		map.put("tag", "");

		List<Map<String, String>> childItems = childlist.get(groupId);

		childItems.add(map);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void 添加子项(int groupId, String title) {
		添加子项(groupId, "", title, "", "", "");
	}

	@Override
	public String 取子项标记(int groupId, int childId) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		return map.get("tag");
	}

	@Override
	public void 置子项标记(int groupId, int childId, String tag) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		map.put("tag", tag);
	}

	@Override
	public String 取子项图片(int groupId, int childId) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		return map.get("image");
	}

	@Override
	public void 置子项图片(int groupId, int childId, String image) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		map.put("image", image);
		adapter.notifyDataSetChanged();
	}

	@Override
	public String 取子项标题(int groupId, int childId) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		return map.get("title");
	}

	@Override
	public void 置子项标题(int groupId, int childId, String title) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		map.put("title", title);
		adapter.notifyDataSetChanged();
	}

	@Override
	public String 取子项信息(int groupId, int childId) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		return map.get("info");
	}

	@Override
	public void 置子项信息(int groupId, int childId, String info) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		map.put("info", info);
		adapter.notifyDataSetChanged();
	}

	@Override
	public String 取子项按钮图片(int groupId, int childId) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		return map.get("buttonImage");
	}

	@Override
	public void 置子项按钮图片(int groupId, int childId, String buttonImage) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		map.put("buttonImage", buttonImage);
		adapter.notifyDataSetChanged();
	}

	@Override
	public String 取子项按钮标题(int groupId, int childId) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		return map.get("buttonTitle");
	}

	@Override
	public void 置子项按钮标题(int groupId, int childId, String buttonTitle) {
		List<Map<String, String>> childItems = childlist.get(groupId);
		Map<String, String> map = childItems.get(childId);

		map.put("buttonTitle", buttonTitle);
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 取子项总数(int groupId) {
		List<Map<String, String>> childItems = childlist.get(groupId);

		return childItems.size();
	}

	@Override
	public void 删除子项(int groupId, int childId) {
		List<Map<String, String>> childItems = childlist.get(groupId);

		childItems.remove(childId);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void 清空子项(int groupId) {
		List<Map<String, String>> childItems = childlist.get(groupId);

		childItems.clear();
		adapter.notifyDataSetChanged();
	}

	@Override
	public void 选中子项(int groupPosition, int childPosition, boolean shouldExpandGroup) {
		expandableListView.setSelectedChild(groupPosition, childPosition, shouldExpandGroup);
	}

	class ExpandableAdapter extends BaseExpandableListAdapter {
		private Context context;

		public final class groupview {
			public TextView title;
			public TextView info;
		}

		public final class childItemview {
			public ImageView image;
			public LinearLayout content;
			public TextView title;
			public TextView info;
			public Button button;
		}

		public ExpandableAdapter(Context context) {
			this.context = context;
		}

		@Override
		public int getGroupCount() {
			return groups.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groups.get(groupPosition).get("title");
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			List<Map<String, String>> childItems = childlist.get(groupPosition);

			return childItems.size();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			List<Map<String, String>> childItems = childlist.get(groupPosition);
			Map<String, String> map = childItems.get(childPosition);

			return map.get("title");
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			groupview gv;

			if (convertView == null) {
				gv = new groupview();
				LinearLayout layout = new LinearLayout(context);

				layout.setOrientation(0);
				layout.setVerticalGravity(16);

				LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(-2, -1);

				param1.weight = 1.0f;
				gv.title = new TextView(context);
				gv.title.setGravity(19);
				gv.title.setPadding(toPixels(50), 0, 0, 0);
				layout.addView(gv.title, param1);

				LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(-2, toPixels(50));

				param2.setMargins(toPixels(5), 0, toPixels(5), 0);
				gv.info = new TextView(context);
				gv.info.setGravity(21);
				layout.addView(gv.info, param2);
				convertView = layout;
				convertView.setTag(gv);
			} else {
				gv = (groupview) convertView.getTag();
			}

			gv.title.setText(groups.get(groupPosition).get("title"));
			gv.title.setTextSize(groupTitletextSize);
			gv.title.setTextColor(groupTitletextColor);
			gv.info.setText(groups.get(groupPosition).get("info"));
			gv.info.setTextSize(groupInfotextSize);
			gv.info.setTextColor(groupInfotextColor);
			return convertView;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View view, ViewGroup parent) {
			childItemview civ;

			if (groups.size() == 0) {
				return null;
			}

			if (view == null) {
				civ = new childItemview();
				LinearLayout linearLayout = new LinearLayout(context);

				linearLayout.setOrientation(0);
				linearLayout.setVerticalGravity(16);

				LinearLayout.LayoutParams image = new LinearLayout.LayoutParams(toPixels(50), toPixels(50));

				image.setMargins(toPixels(5), toPixels(5), toPixels(5), toPixels(5));
				civ.image = new ImageView(context);
				civ.image.setLayoutParams(image);
				linearLayout.addView(civ.image, image);
				LinearLayout layout2 = new LinearLayout(context);

				civ.content = layout2;

				LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(-2, -2);

				param2.weight = 1.0f;
				layout2.setOrientation(1);

				LinearLayout.LayoutParams title = new LinearLayout.LayoutParams(-1, -1);

				title.weight = 1.0f;
				civ.title = new TextView(context);
				civ.title.setSingleLine(true);
				civ.title.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
				civ.title.setGravity(19);
				civ.title.setPadding(0, toPixels(5), 0, toPixels(5));
				layout2.addView(civ.title, title);

				LinearLayout.LayoutParams info = new LinearLayout.LayoutParams(-1, -1);

				info.weight = 1.0f;
				civ.info = new TextView(context);
				civ.info.setSingleLine(true);
				civ.info.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
				civ.info.setGravity(19);
				civ.info.setPadding(0, toPixels(5), 0, toPixels(5));
				layout2.addView(civ.info, info);
				linearLayout.addView(layout2, param2);

				LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(toPixels(55), toPixels(35));

				button.setMargins(toPixels(5), 0, toPixels(5), 0);
				civ.button = new Button(context);
				civ.button.setLayoutParams(button);
				linearLayout.addView(civ.button, button);
				view = linearLayout;
				view.setTag(civ);
			} else {
				civ = (childItemview) view.getTag();
			}

			List<Map<String, String>> childItems = childlist.get(groupPosition);
			Map<String, String> map = childItems.get(childPosition);
			String image = map.get("image");
			boolean hasImage = !TextUtils.isEmpty(image);

			civ.image.setVisibility(hasImage ? View.VISIBLE : View.GONE);
			civ.image.setBackgroundDrawable(hasImage ? getDrawable(image) : null);
			civ.content.setPadding(hasImage ? 0 : toPixels(10), 0, 0, 0);

			civ.title.setText(map.get("title"));
			civ.title.setTextSize(childTitletextSize);
			civ.title.setTextColor(childTitletextColor);

			String info = map.get("info");

			civ.info.setVisibility(TextUtils.isEmpty(info) ? View.GONE : View.VISIBLE);
			civ.info.setText(info);
			civ.info.setTextSize(childInfotextSize);
			civ.info.setTextColor(childInfotextColor);

			civ.button.setBackgroundDrawable(getDrawable(map.get("buttonImage")));
			civ.button.setFocusable(false);
			civ.button.setTextSize(childButtontextSize);
			civ.button.setTextColor(childButtontextColor);
			civ.button.setGravity(17);
			civ.button.setText(map.get("buttonTitle"));
			civ.button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					子项按钮被单击(groupPosition, childPosition);
				}
			});

			if (显示子项按钮()) {
				civ.button.setVisibility(0);
			} else {
				civ.button.setVisibility(8);
			}

			return view;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		private Drawable getDrawable(String imagePath) {
			Drawable drawable = null;

			if (imagePath.length() > 0) {
				if (imagePath.startsWith("/")) {
					File f = new File(imagePath);

					if (f.exists()) {
						drawable = Drawable.createFromPath(imagePath);
					}
				} else {
					try {
						drawable = Drawable.createFromStream(context.getResources().getAssets().open(imagePath), imagePath);
					} catch (IOException e) {
					}

				}
			}

			return drawable;
		}

		/**
		 * 将布局尺寸由 DIP 换算为当前设备的像素尺寸。
		 *
		 * @param value 布局尺寸
		 * @return 像素尺寸
		 */
		private int toPixels(int value) {
			float scale = context.getResources().getDisplayMetrics().density;

			return (int) ((value * scale) + 0.5f);
		}
	}
}
