package simple.library.wsd.update;

import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;
import simple.runtime.android.MainActivity;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.vector.update_app.HttpManager;
import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.UpdateCallback;
import com.vector.update_app.listener.ExceptionHandler;

/**
 * 应用更新组件的实现。
 * 
 * @author 树先生 xhwsd@qq.com
 */
public final class 应用更新Impl extends 组件Impl implements 应用更新 {
	/*
	WVector/AppUpdate
	仓库地址：https://github.com/WVector/AppUpdate
	版本：3.5.2
	更新：2018-5-25
	*/

	// 构造器
	private MyBuilder builder;
	// 更新应用管理器
	private UpdateAppManager updateAppManager;

	/**
	 * 创建一个新的应用更新组件。
	 *
	 * @param container  容纳组件的容器（对于不可见的组件必须是窗口，不可为 {@code null}）
	 */
	public 应用更新Impl(组件容器 container) {
		super(container);
		builder = new MyBuilder();

		// 全局异常捕获
		builder.handleException(new ExceptionHandler() {
			@Override
			public void onException(Exception e) {
				e.printStackTrace();
			}
		});
	}

	/* 应用更新 实现 */

    public void 检测更新前() {
		EventDispatcher.dispatchEvent(this, "检测更新前");
	}

    public void 检测更新后() {
		EventDispatcher.dispatchEvent(this, "检测更新后");
	}

	public void 已检测到更新(String update, String newVersion, String apkFileUrl, String updateLog, String targetSize) {
		EventDispatcher.dispatchEvent(this, "已检测到更新", update, newVersion, apkFileUrl, updateLog, targetSize);
	}

	public void 未检测到更新() {
		EventDispatcher.dispatchEvent(this, "未检测到更新");
	}

	public boolean 提交请求方式() {
		return builder.isPost();
	}

	public void 提交请求方式(boolean isPost) {
		builder.setPost(isPost);
	}

	public boolean 仅无线网络下载() {
		return builder.isOnlyWifi();
	}

	public void 仅无线网络下载(boolean onlyWifi) {
		builder.setOnlyWifi(onlyWifi);
	}

	public String 下载保存路径() {
		return builder.getTargetPath();
	}

	public void 下载保存路径(String targetPath) {
		builder.setTargetPath(targetPath);
	}

	public int 主题颜色() {
		return builder.getThemeColor();
	}

	public void 主题颜色(int themeColor) {
		builder.setThemeColor(themeColor);
	}

	public int 顶部图片() {
		return builder.getTopPic();
	}

	public void 顶部图片(int topPic) {
		builder.setTopPic(topPic);
	}

	public boolean 禁止通知栏进度() {
		return builder.isDismissNotificationProgress();
	}

	public void 禁止通知栏进度(boolean dismissNotificationProgress) {
		builder.dismissNotificationProgress(dismissNotificationProgress);
	}

	public boolean 显示忽略版本() {
		return builder.isShowIgnoreVersion();
	}

	public void 显示忽略版本(boolean showIgnoreVersion) {
		builder.showIgnoreVersion(showIgnoreVersion);
	}

	public void 添加请求参数(String name, String value) {
		Map<String, String> params = builder.getParams();
		if (params == null) {
			params = new HashMap<String, String>();
			builder.setParams(params);
		}
		params.put(name, value);
	}

	public void 检验更新(String updateUrl) {
		// 必须设置，当前Activity
		builder.setActivity(MainActivity.getContext());
		// 必须设置，实现httpManager接口的对象
		builder.setHttpManager((HttpManager) new OkGoUpdateHttpUtil());
		// 必须设置，更新地址
		builder.setUpdateUrl(updateUrl);
		// 检测是否有新版本
		builder.build().checkNewApp(new UpdateCallback() {

			/**
			 * 网络请求之前
			 */
			@Override
			public void onBefore() {
				检测更新前();
			}
			
            /**
             * 网路请求之后
             */
			public void onAfter() {
				检测更新后();
			}

			/**
			 * 解析json,自定义协议
			 *
			 * @param json 服务器返回的json
			 * @return UpdateAppBean
			 */
			@Override
			protected UpdateAppBean parseJson(String json) {
				UpdateAppBean updateAppBean = new UpdateAppBean();
				try {
					JSONObject jsonObject  = new JSONObject(json);
					// （必须）是否更新 Yes,No
					updateAppBean.setUpdate(jsonObject.optString("update"));
					//（必须）新版本号
					updateAppBean.setNewVersion(jsonObject.optString("new_version"));
					//（必须）下载地址
					updateAppBean.setApkFileUrl(jsonObject.optString("apk_file_url"));
					// 更新日志
					updateAppBean.setUpdateLog(jsonObject.optString("update_log"));
					// 大小，不设置不显示大小，可以不设置
					if (jsonObject.has("target_size")) {
						updateAppBean.setTargetSize(jsonObject.optString("target_size"));
					}
					// 是否强制更新，可以不设置
					if (jsonObject.has("constraint")) {
						updateAppBean.setConstraint(jsonObject.optBoolean("constraint"));
					}
				} catch (Exception exception) {
					exception.printStackTrace();
				}
				return updateAppBean;
			}

			/**
			 * 有新版本
			 *
			 * @param updateApp 新版本信息
			 * @param updateAppManager app更新管理器
			 */
			@Override
			public void hasNewApp(UpdateAppBean updateApp, UpdateAppManager updateAppManager) {
				应用更新Impl.this.updateAppManager = updateAppManager;
				已检测到更新(
					updateApp.getUpdate(),
					updateApp.getNewVersion(),
					updateApp.getApkFileUrl(),
					updateApp.getUpdateLog(),
					updateApp.getTargetSize()
				);
			}
			
			/**
			 * 没有新版本
			 */
			@Override
			public void noNewApp(String param1String) {
				未检测到更新();
			}
		});
	}

	public void 显示更新对话框() {
		if (updateAppManager != null) {
			updateAppManager.showDialogFragment();
		}
	}

	/**
	 * UpdateAppManager.Builder 中某些方法不友好，重写下！
	 */
	private class MyBuilder extends UpdateAppManager.Builder {
		// 是否仅WIFI下载
		private boolean onlyWifi;
		// 是否禁止显示通知栏进度
		private boolean dismissNotificationProgress;
		// 是否显示忽略版本
		private boolean showIgnoreVersion;

		public MyBuilder setOnlyWifi(boolean onlyWifi) {
            this.onlyWifi = onlyWifi;
            return this;
        }

		@Override
        public boolean isOnlyWifi() {
            return onlyWifi;
		}
		
		public MyBuilder dismissNotificationProgress(boolean dismissNotificationProgress) {
            this.dismissNotificationProgress = dismissNotificationProgress;
            return this;
		}
		
		@Override
        public boolean isDismissNotificationProgress() {
            return dismissNotificationProgress;
		}
		
        public MyBuilder showIgnoreVersion(boolean showIgnoreVersion) {
			this.showIgnoreVersion = showIgnoreVersion;
			return this;
		}
		
		@Override
        public boolean isShowIgnoreVersion() {
            return showIgnoreVersion;
        }
	}
}
