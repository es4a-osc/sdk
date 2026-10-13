package simple.library.wsd.update;

import simple.runtime.annotations.ManifestNodes;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.组件;

/**
 * 应用更新组件。
 *
 * <p>基于{@link https://github.com/WVector/AppUpdate} 实现。
 * <p>注意清单宏{@code 应用更新.应用程序标识}必须为当前应用包名
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	// 允许应用程序访问网络连接状态。
	"android.permission.ACCESS_NETWORK_STATE," +
	// 允许应用程序访问网络连接状态。
	"android.permission.ACCESS_NETWORK_STATE," +
	// 允许应用程序打开网络套接字。
	"android.permission.INTERNET," +
	// 允许应用程序写入外部存储。
	"android.permission.WRITE_EXTERNAL_STORAGE," +
	// 允许应用程序读取外部存储。
	"android.permission.READ_EXTERNAL_STORAGE," +
	// 允许应用程序请求安装应用包。
	"android.permission.REQUEST_INSTALL_PACKAGES"
)
@ManifestNodes(applicationXml =
	"<service android:name=\"com.vector.update_app.service.DownloadService\" />" +
	"<provider" +
		" android:name=\"com.vector.update_app.UpdateFileProvider\"" +
		" android:authorities=\"${应用程序标识}.fileProvider\"" +
		" android:exported=\"false\"" +
		" android:grantUriPermissions=\"true\" >" +
		"<meta-data" +
			" android:name=\"android.support.FILE_PROVIDER_PATHS\"" +
			" android:resource=\"@xml/new_app_file_paths\" />" +
	"</provider>"
)
public interface 应用更新 extends 组件 {

	/* 事件 */

	/**
	 * 网络请求之前
	 */
	@SimpleEvent
	void 检测更新前();

	/**
	 * 网路请求之后
	 */
	@SimpleEvent
	void 检测更新后();

	/**
	 * 有新版本
	 */
	@SimpleEvent
	void 已检测到更新(String update, String newVersion, String apkFileUrl, String updateLog, String targetSize);

	/**
	 * 没有新版本
	 */
	@SimpleEvent
	void 未检测到更新();

	/* 属性 */

	/**
	 * 是否POST请求方式。
	 *
	 * @return 是否使用 POST 请求
	 */
	@SimpleProperty
	boolean 提交请求方式();

	/**
	 * 设置请求方式。
	 *
	 * @param isPost 真使用 POST，假使用 GET
	 */
	void 提交请求方式(boolean isPost);

	/**
	 * 是否仅在WIFI情况下载。
	 *
	 * @return 是否仅通过 Wi-Fi 下载
	 */
	@SimpleProperty
	boolean 仅无线网络下载();

	/**
	 * 是否仅在WIFI情况下载。
	 *
	 * @param onlyWifi 是否仅通过 Wi-Fi 下载
	 */
	@SimpleProperty
	void 仅无线网络下载(boolean onlyWifi);

	/**
	 * 读取 APK 下载保存路径。
	 *
	 * @return 下载保存路径
	 */
	@SimpleProperty
	String 下载保存路径();

	/**
	 * 设置 APK 下载保存路径。
	 *
	 * @param targetPath 下载保存路径
	 */
	@SimpleProperty
	void 下载保存路径(String targetPath);

	/**
	 * 为按钮，进度条设置颜色。
	 *
	 * @return 主题颜色值
	 */
	@SimpleProperty
	int 主题颜色();

	/**
	 * 为按钮，进度条设置颜色。
	 *
	 * @param themeColor 主题颜色值
	 */
	@SimpleProperty
	void 主题颜色(int themeColor);

	/**
	 * 设置头部，不设置显示默认的图片，设置图片后自动识别主色调，然后为按钮，进度条设置颜色
	 *
	 * @return 顶部图片资源编号
	 */
	@SimpleProperty
	int 顶部图片();

	/**
	 * 设置头部，不设置显示默认的图片，设置图片后自动识别主色调，然后为按钮，进度条设置颜色
	 *
	 * @param topPic 顶部图片资源编号
	 */
	@SimpleProperty
	void 顶部图片(int topPic);

	/**
	 * 是否禁止显示通知栏进度条
	 *
	 * @return 是否隐藏通知栏下载进度
	 */
	@SimpleProperty
	boolean 禁止通知栏进度();

	/**
	 * 是否禁止显示通知栏进度条
	 *
	 * @param dismissNotificationProgress 是否隐藏通知栏下载进度
	 */
	@SimpleProperty
	void 禁止通知栏进度(boolean dismissNotificationProgress);

	/**
	 * 是否显示忽略版本选项。
	 *
	 * @return 是否显示忽略版本选项
	 */
	@SimpleProperty
	boolean 显示忽略版本();

	/**
	 * 设置忽略版本选项的显示状态。
	 *
	 * @param showIgnoreVersion 是否显示忽略版本选项
	 */
	@SimpleProperty
	void 显示忽略版本(boolean showIgnoreVersion);

	/* 方法 */

	/**
	 * 添加自定义参数，默认version=1.0.0（app的versionName）；apkKey=唯一表示（在AndroidManifest.xml配置）
	 *
	 * @param name 参数名
	 * @param value 参数值
	 */
	@SimpleFunction
	void 添加请求参数(String name, String value);

	/**
	 * 检测是否需要更新
	 *
	 * @param updateUrl 更新检测地址
	 */
	@SimpleFunction
	void 检验更新(String updateUrl);

	/**
	 * 已检测到更新后显示更新对话框。
	 */
	@SimpleFunction
	void 显示更新对话框();
}
