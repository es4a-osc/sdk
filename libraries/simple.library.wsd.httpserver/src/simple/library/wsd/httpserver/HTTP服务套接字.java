package simple.library.wsd.httpserver;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.variants.Variant;
import simple.runtime.helpers.ConvHelpers;

import com.koushikdutta.async.http.WebSocket;

/**
 * HTTP服务套接字
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class HTTP服务套接字 {

	// 套接字
	private WebSocket socket;

	public HTTP服务套接字(WebSocket socket) {
		this.socket = socket;
	}

	/**
	 * 发送数据
	 * 
	 * @param data 数据，支持文本型和字节数组。
	 */
	@SimpleFunction
	public void 发送(Variant data) {
		Object object = ConvHelpers.variant2object(data);
		if (object instanceof String) {
			socket.send((String) object);
		} else if (object instanceof byte[]) {
			socket.send((byte[]) object);
		} else {
			throw new IllegalArgumentException("数据类型无效");
		}
	}

	/**
	 * 发送数据
	 * 
	 * @param bytes 字节数组
	 * @param offset 偏移位置
	 * @param length 数据长度
	 */
	@SimpleFunction
	public void 发送(byte [] bytes, int offset, int length) {
		socket.send(bytes, offset, length);
	}

	/**
	 * 向客户端发送探测
	 * 
	 * @param message 消息
	 */
	@SimpleFunction
	public void 探测(String message) {
		socket.ping(message);
	}

	/**
	 * 向客户端答复探测
	 * 
	 * @param message 消息
	 */
	@SimpleFunction
	public void 答复(String message) {
		socket.pong(message);
	}

	/**
	 * 是否正在缓存
	 * 
	 * @return 正在缓存返回真，否则返回假
	 */
	@SimpleFunction
	public boolean 正在缓存() {
		return socket.isBuffering();
	}
}
