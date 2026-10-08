package simple.library.wsd.httpserver;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.Variant;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.callback.CompletedCallback;

/**
 * HTTP服务
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@UsesPermissions(permissionNames = 
		// 访问网络
		"android.permission.INTERNET" )
public final class HTTP服务 {
	/*
	github - AndroidAsync：
	https://github.com/koush/AndroidAsync

	github - AndroidHttpServer：
	https://github.com/qianxin2016/AndroidHttpServer/tree/master/app
	*/

	// HTTP服务
	private AsyncHttpServer server = new AsyncHttpServer();

	/**
	 * 注销路由
	 * 
	 * @param action 请求方式，如：GET、POST、PATCH、DELETE等
	 * @param regex 匹配请求规则
	 */
	@SimpleFunction
   	public void 注销路由(String action, String regex) {
		server.removeAction(action, regex);
   	}

	/**
	 * 注册路由
	 * 
	 * @param action 请求方式，如：GET、POST、PATCH、DELETE等
	 * @param regex 匹配请求规则
	 * @param unit 响应单元实例
	 * @param name 响应方法名，过程结构{@code 过程名(收到请求 为 Web请求, 响应客户 为 Web响应, 附加标记 为 变体型)}
	 */
	@SimpleFunction
   	public void 注册路由(String action, String regex, Object unit, String name) {
		注册路由(action, regex, unit, name, null);
   	}
	
	/**
	 * 注册路由
	 * 
	 * @param action 请求方式，如：GET、POST、PATCH、DELETE等
	 * @param regex 匹配请求规则
	 * @param unit 响应单元实例
	 * @param name 响应方法名，过程结构{@code 过程名(收到请求 为 Web请求, 响应客户 为 Web响应, 附加标记 为 变体型)}
	 * @param mark 附加标记
	 */
	@SimpleFunction
   	public void 注册路由(String action, String regex, Object unit, String name, Variant mark) {
		Variant object = ObjectVariant.getObjectVariant(unit);
		server.addAction(action, regex, new HttpServerRequestCallback() {
			@Override
			public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Variant[] args = new Variant[3];
				args[0] = ObjectVariant.getObjectVariant(new HTTP服务请求(request));
				args[1] = ObjectVariant.getObjectVariant(new HTTP服务响应(response));
				args[2] = mark;
				object.function(name, args);
			}
		});
   	}

	/**
	 * 启动绑定监听端口
	 * 
	 * @param port 绑定监听端口
	 */
	@SimpleFunction
    public void 启动(int port) {
		server.listen(port);
	}
	
	/**
	 * 停止服务
	 */
	@SimpleFunction
    public void 停止() {
        server.stop();
	}

	/**
	 * 注册套接字
	 * 
	 * @param regex 匹配请求规则
	 * @param unit 响应单元实例
	 * @param name 响应方法名，过程结构{@code 过程名(套接字 为 Web套接字, 事件类型 为 文本型, 事件数据 为 变体型, 附加标记 为 变体型)}
	 */
    public void 注册套接字(String regex, Object unit, String name) {
		注册套接字(regex, unit, name, null);
	}

	/**
	 * 注册套接字
	 * 
	 * @param regex 匹配请求规则
	 * @param unit 响应单元实例
	 * @param name 响应方法名，过程结构{@code 过程名(套接字 为 Web套接字, 事件类型 为 文本型, 事件数据 为 变体型, 附加标记 为 变体型)}
	 * @param mark 附加标记
	 */
    public void 注册套接字(String regex, Object unit, String name, Variant mark) {
		Variant object = ObjectVariant.getObjectVariant(unit);
        server.websocket(regex, new AsyncHttpServer.WebSocketRequestCallback() {
			@Override
			public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
				Variant[] args = new Variant[4];
				args[0] = ObjectVariant.getObjectVariant(new HTTP服务套接字(webSocket));
				args[1] = StringVariant.getStringVariant("connected");
				args[2] = ObjectVariant.getObjectVariant(new HTTP服务请求(request));
				args[3] = mark;
				object.function(name, args);

				// 设置关闭回调
				webSocket.setClosedCallback(new CompletedCallback() {
					@Override
					public void onCompleted(Exception ex) {
						args[1] = StringVariant.getStringVariant("closed");
						args[2] = StringVariant.getStringVariant(ex.getMessage());
						object.function(name, args);
					}
				});
				
				webSocket.setStringCallback(new WebSocket.StringCallback() {
					@Override
					public void onStringAvailable(String s) {
						args[1] = StringVariant.getStringVariant("string");
						args[2] = StringVariant.getStringVariant(s);
						object.function(name, args);
					}
				}); 
			}
		});
	}
}
