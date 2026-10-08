package simple.library.wsd.httpserver;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.JSON值;
import simple.runtime.helpers.ConvHelpers;
import simple.runtime.variants.Variant;

import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import org.json.JSONObject;

/**
 * HTTP服务响应
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public class HTTP服务响应 {

	// 响应实例
	private AsyncHttpServerResponse response;

	public HTTP服务响应(AsyncHttpServerResponse response) {
		this.response = response;
	}

	/**
	 * 结束本次HTTP响应
	 */
	@SimpleFunction
	public void 结束() {
		response.end();
	}

	/**
	 * 向客户端发送数据
	 * 
	 * @param data 数据，支持文本型和JSON值。
	 */
	@SimpleFunction
	public void 发送(Variant data) {
		Object object = ConvHelpers.variant2object(data);
		if (object instanceof String) {
			response.send((String) object);
		} else if (object instanceof JSON值) {
			object = ((JSON值) object).getObject();
			if (object instanceof JSONObject) {
				response.send((JSONObject) object);
			} else {
				throw new IllegalArgumentException("JSON值必须为JSON对象");
			}
		} else {
			throw new IllegalArgumentException("数据类型无效");
		}
	}

	/**
	 * 向客户端发送数据
	 * 
	 * @param contentType 内容类型，如“application/json”、“ext/plain”、“text/html”等
	 * @param data 数据，支持文本型和字节数组。
	 */
	@SimpleFunction
	public void 发送(String contentType, Variant data) {
		Object object = ConvHelpers.variant2object(data);
		if (object instanceof String) {
			response.send(contentType, (String) object);
		} else if (object instanceof byte[]) {
			response.send(contentType, (byte[]) object);
		} else {
			throw new IllegalArgumentException("数据类型无效");
		}
	}

	/**
	 * 取响应代码
	 */
	@SimpleFunction
	public int 取代码() {
		return response.code();
	}

	/**
	 * 置响应代码
	 * 
	 * @param code 代码
	 */
	@SimpleFunction
	public void 置代码(int code) {
		response.code(code);
	}

	/**
	 * 有无协议头
	 * 
	 * @return 有无
	 */
	@SimpleFunction
	public boolean 有无协议头(String header) {
		// 这样兼容大小写
		return response.getHeaders().getAll(header) != null;
	}

	/**
	 * 取协议头名
	 * 
	 * @return 协议头名
	 */
	@SimpleFunction
	public String[] 取协议头名() {
		return response.getHeaders().getMultiMap().keySet().toArray(new String[0]);
	}
	
	/**
	 * 取协议头值
	 * 
	 * @param header 协议头名
	 * @return 首个值
	 */
	@SimpleFunction
	public String 取协议头值(String header) {
		return response.getHeaders().get(header);
	}

	/**
	 * 取协议头所有值
	 * 
	 * @param header 协议头名
	 * @return 所有值
	 */
	@SimpleFunction
	public String[] 取协议头所有值(String header) {
		return response.getHeaders().getAll(header).toArray(new String[0]);
	}

	/**
	 * 置协议头值
	 * 
	 * @param header 协议头名
	 * @param value 协议头值
	 */
	@SimpleFunction
	public void 置协议头值(String header, String value) {
		response.getHeaders().set(header, value);
	}

	/**
	 * 添加协议头值
	 * 
	 * @param header 协议头名
	 * @param value 协议头值
	 */
	@SimpleFunction
	public void 添加协议头值(String header, String value) {
		response.getHeaders().add(header, value);
	}

	/**
	 * 删除协议头
	 * 
	 * @param header 协议头名
	 */
	@SimpleFunction
	public void 删除协议头(String header) {
		response.getHeaders().removeAll(header);
	}

	/**
	 * 置内容类型
	 * 
	 * @param contentType 内容类型，如“application/json”、“ext/plain”、“text/html”等
	 */
	@SimpleFunction
    public void 置内容类型(String contentType) {
		response.setContentType(contentType);
	}

	/**
	 * 重定向
	 * 
	 * @param location 位置
	 */
	@SimpleFunction
	public void 重定向(String location) {
		response.redirect(location);
	}
}
