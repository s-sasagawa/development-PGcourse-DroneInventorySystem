package com.digitalojt.web.controller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.digitalojt.web.consts.LogMessage;
import com.digitalojt.web.consts.ModelAttributeContents;
import com.digitalojt.web.consts.Region;
import com.digitalojt.web.consts.UrlConsts;
import com.digitalojt.web.entity.CenterInfo;
import com.digitalojt.web.form.CenterInfoForm;
import com.digitalojt.web.service.CenterInfoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 在庫センター情報画面のコントローラークラス
 * 
 * @author dotlife
 *
 */
@Controller
@RequiredArgsConstructor
public class CenterInfoController extends AbstractController {

	/** センター情報 サービス */
	private final CenterInfoService centerInfoService;

	/**
	 * 都道府県Enumをリストに変換
	 * 
	 * @return
	 */
	@ModelAttribute(ModelAttributeContents.REGIONS)
	public List<Region> populateRegions() {
		return Arrays.asList(Region.values());
	}

	/**
	 * 初期表示
	 * 
	 * @param model
	 * @retur
	 */
	@GetMapping(UrlConsts.CENTER_INFO)
	public String index(Model model) {
		logStart(LogMessage.HTTP_GET);

		// 在庫センター情報画面に表示するデータを取得
		List<CenterInfo> centerInfoList = centerInfoService.getCenterInfoData();

		// 画面表示用に商品情報リストをセット
		model.addAttribute(ModelAttributeContents.CENTER_INFO_LIST, centerInfoList);

		logEnd(LogMessage.HTTP_GET);

		return UrlConsts.CENTER_INFO_INDEX;
	}

	/**
	 * 検索結果表示
	 * 
	 * @param model
	 * @param form
	 * @return
	 */
	@GetMapping(UrlConsts.CENTER_INFO_SEARCH)
	public String search(Model model, @Valid CenterInfoForm form, BindingResult bindingResult) {
		logStart(LogMessage.HTTP_GET);

		// 入力値のバリデーションチェック
		if (bindingResult.hasErrors()) {
			handleValidationError(model, bindingResult, form);
			return UrlConsts.CENTER_INFO_INDEX;
		}

		// 検索条件に基づいて在庫センター情報を取得
		List<CenterInfo> centerInfoList = centerInfoService.getCenterInfoData(form.getCenterName(),form.getManagerName(), form.getRegion());

		// 画面表示用に商品情報リストをセット
		model.addAttribute(ModelAttributeContents.CENTER_INFO_LIST, centerInfoList);

		logEnd(LogMessage.HTTP_GET);

		return UrlConsts.CENTER_INFO_INDEX;
	}

	/**
	 * バリデーションエラー処理
	 * 
	 * @param model
	 * @param bindingResult
	 * @param form
	 */
	private void handleValidationError(Model model, BindingResult bindingResult, CenterInfoForm form) {
		// エラーメッセージをリストに格納
		List<String> objectErrorNames = List.of("form");
		List<ObjectError> sortedErrors = getSortErrors(bindingResult,CenterInfoForm.class,objectErrorNames);
		

		StringBuilder errorMsg = new StringBuilder();
		String message = sortedErrors.stream()
			.map(error -> {
				if (error instanceof FieldError fieldError ) {
					return fieldError.getDefaultMessage();
				} else {
				return error.getDefaultMessage();
				}
            })
			.collect(Collectors.joining("\r\n"));
		errorMsg.append(message);

		// エラーメッセージをモデルに追加
		model.addAttribute(LogMessage.FLASH_ATTRIBUTE_ERROR, errorMsg.toString());

		logValidationError(LogMessage.HTTP_POST, form + " " + errorMsg.toString());
	}
	
	
	/**
	 * エラーメッセージソート処理
	 * 
	 * @param bindingResult
	 * @param formClass
	 * @param objectErrorNames
	 */
	private static List<ObjectError> getSortErrors(BindingResult bidingresult, Class<?> formClass, List<String> objectErrorNames) {
		
		List<String> errorOrder = getErrorOrder(formClass,objectErrorNames);
		
		return bidingresult.getAllErrors().stream()
				//センター名,管理者名,都道府県名になるようにソート
				//オブジェクトエラー・フィールドエラー関係なく、項目順に並び替える
				.sorted(Comparator.comparingInt(error -> {
					String Key;
					if (error instanceof FieldError fieldError) {
						Key = error.getDefaultMessage();
						//Key = fieldError.getField();
						if (Key.contains("センター名")) {
							return 0;
						} else if (Key.contains("管理者名")){ 
							return 1;
						} 
						
					} else {
						Key = error.getDefaultMessage();
						//Key = error.getObjectName();
						if (Key.contains("センター名")) {
							return 0;
						} else if (Key.contains("管理者名")){ 
							return 1;
						}
					}
					int index = errorOrder.indexOf(Key);
					return index >= 0 ? index : Integer.MAX_VALUE;
				}))
				.collect(Collectors.toList());
	}
		
	
	/**
	 * エラーリスト定義
	 * 
	 * @param clazz
	 * @param objectErrorNames
	 */
	private static  List<String> getErrorOrder(Class<?> clazz, List<String> objectErrorNames) {
		List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
		.map(Field::getName)
		.collect(Collectors.toList());
	
		List<String>  allErrorNames = new ArrayList<>(fieldNames);
		allErrorNames.addAll(objectErrorNames);
		return allErrorNames;
	}
}
	

	