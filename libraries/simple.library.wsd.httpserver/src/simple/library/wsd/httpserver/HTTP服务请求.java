package simple.library.wsd.httpserver;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.collections.JSON值;
import simple.runtime.collections.哈希表;
import simple.runtime.helpers.ConvHelpers;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.Variant;

import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * HTTP服务请求
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class HTTP服务请求 {

	// 请求实例
	private AsyncHttpServerRequest request;

	public HTTP服务请求(AsyncHttpServerRequest request) {
		this.request = request;
	}

	/**
	 * 有无协议头
	 * 
	 * @param header 协议头名
	 * @return 有无
	 */
	@SimpleFunction
	public boolean 有无协议头(String header) {
		// 兼容大小写
		return request.getHeaders().getAll(header) != null;
	}

	/**
	 * 取所有协议头名
	 * 
	 * @return 协议头名
	 */
	@SimpleFunction
	public String[] 取协议头名() {
		return request.getHeaders().getMultiMap().keySet().toArray(new String[0]);
	}

	/**
	 * 取指定协议头名的值
	 * 
	 * @param header 协议头名
	 * @return 协议头值
	 */
	@SimpleFunction
	public String 取协议头值(String header) {
		return request.getHeaders().get(header);
	}

	/**
	 * 取指定协议头名的所有值
	 * 
	 * @param header 协议头名
	 * @return 协议头所有值
	 */
	@SimpleFunction
	public String[] 取协议头所有值(String header) {
		return request.getHeaders().getAll(header).toArray(new String[0]);
	}

	/**
	 * 取匹配组计数
	 * 
	 * @return 匹配组计数
	 */
	@SimpleFunction
	public int 取匹配计数() {
		return request.getMatcher().groupCount();
	}

	/**
	 * 取注册路由的正则表达式匹配结果
	 * 
	 * @param group 捕获组索引，0为整个匹配结果，后续为子匹配文本
	 * @return 匹配文本
	 */
	@SimpleFunction
	public String 取匹配文本(int group) {
		return request.getMatcher().group(group);
	}

	/**
	 * 取主体内容类型
	 * 
	 * @return 内容类型
	 */
	@SimpleFunction
	public String 取主体内容类型() {
		return request.getBody().getContentType();
	}

	/**
	 * 取主体长度
	 * 
	 * @return 长度
	 */
	@SimpleFunction
	public int 取主体长度() {
		return request.getBody().length();
	}

	/**
	 * 取已解析的主体数据。分段接收的表单只包含当前已解析的字段。
	 *
	 * @return 文本、JSON值或以文本数组保存各字段值的哈希表；无解析结果时为空
	 */
	@SimpleFunction
	public Variant 取主体数据() {
		AsyncHttpRequestBody<?> body = request.getBody();
		Object data = body == null ? null : body.get();
		if (data instanceof JSONObject) {
			return ObjectVariant.getObjectVariant(new JSON值(data));
		}
		
		if (data instanceof Multimap) {
			哈希表 result = new 哈希表();
			for (Map.Entry<String, List<String>> entry : ((Multimap) data).entrySet()) {
				result.加入(
					entry.getKey(),
					ArrayVariant.getArrayVariant(entry.getValue().toArray(new String[0]))
				);
			}
			return ObjectVariant.getObjectVariant(result);
		}
		return ConvHelpers.object2variant(data);
	}

	/**
	 * 请求路径属性获取方法
	 * 
	 * @return 请求路径
	 */
	@SimpleProperty
	public String 请求路径() {
		return request.getPath();
	}

	/**
	 * 有无查询
	 * 
	 * @param name 参数名
	 * @return 有无
	 */
	@SimpleFunction
	public boolean 有无查询(String name) {
		return request.getQuery().containsKey(name);
	}

	/**
	 * 取所有查询参数名
	 * 
	 * @return 参数名
	 */
	@SimpleFunction
	public String[] 取查询名() {
		return request.getQuery().keySet().toArray(new String[0]);
	}

	/**
	 * 取指定参数值
	 * 
	 * @param name 参数名
	 * @return 参数值
	 */
	@SimpleFunction
	public String 取查询值(String name) {
		return request.getQuery().getString(name);
	}

	/**
	 * 请求方式属性获取方法
	 * 
	 * @return 请求方式
	 */
	@SimpleProperty
	public String 请求方式() {
		return request.getMethod();
	}
}
