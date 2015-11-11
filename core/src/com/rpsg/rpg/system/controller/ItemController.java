package com.rpsg.rpg.system.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.rpsg.rpg.core.RPG;
import com.rpsg.rpg.core.Setting;
import com.rpsg.rpg.object.base.items.BaseItem;
import com.rpsg.rpg.object.base.items.Equipment;
import com.rpsg.rpg.object.base.items.Item;
import com.rpsg.rpg.object.base.items.Item.ItemForward;
import com.rpsg.rpg.object.base.items.Item.ItemRange;
import com.rpsg.rpg.object.base.items.Spellcard;
import com.rpsg.rpg.object.rpg.Hero;
import com.rpsg.rpg.utils.game.Logger;

/**
 * GDX-RPG 道具核心管理器
 * @author dingjibang
 *
 */
public class ItemController {
	
	private static JsonReader reader = null;
	
	/**
	 * 给当前游戏存档放入一个道具
	 * @param id 道具ID
	 */
	public void put(int id){
		BaseItem baseItem =search(id);
		if(baseItem==null)
			RPG.global.items.add(get(id));
		else
			//如果可叠加的，则数量+1，否则新建实例_(:3」∠)_
			if(baseItem.packable)
				baseItem.count++;
			else
				RPG.global.items.add(get(id));
	}
	
	public void put(BaseItem baseItem){
		put(baseItem.id);
	}
	
	/**
	 * 根据ID从文件里读取出一个Item
	 * @param id id键
	 * @return
	 */
	public BaseItem get(int id){
		return get(id,BaseItem.class);
	}
	
	/**
	 * 根据ID从文件里读取出一个Item，并且造型
	 * @param id id键
	 * @param _cType 类型
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(int id,Class<T> _cType){
		initReader();
		JsonValue result=reader.parse(Gdx.files.internal(Setting.SCRIPT_DATA_ITEM+id+".grd"));
		String type=result.getString("type");
		try {
			BaseItem baseItem;
			
			//差别处理
			if(type.equalsIgnoreCase(Equipment.class.getSimpleName())){
				Equipment e =(Equipment)(baseItem=new Equipment());
				e.description2 = result.has("description2")?result.getString("description2"):"";
				e.onlyFor=(Class<? extends Hero>) (result.has("onlyFor")?Class.forName("com.rpsg.rpg.game.hero."+result.getString("onlyFor")):null);
				e.equipType=result.getString("equipType");
				
				//读取装备属性
				readProp(e, result.get("effect"));
				
			}else if(type.equalsIgnoreCase(Spellcard.class.getSimpleName())){				//TODO
				Spellcard e =(Spellcard)(baseItem=new Spellcard());
				e.description2 = result.has("description2")?result.getString("description2"):"";
			}else{
				Item item = (Item)(baseItem = new Item());
				item.forward = result.has("forward")?ItemForward.valueOf(result.getString("forward")):ItemForward.friend;
				item.range = result.has("range")?ItemRange.valueOf(result.getString("range")):ItemRange.one;
				
				//读取道具属性
				readProp(item, result.get("effect"));
			}
			
			baseItem.id = id;
			baseItem.disable = false;
			baseItem.description = result.getString("description");
			baseItem.throwable = result.has("throwable") ? result.getBoolean("throwable") : true;
			baseItem.name = result.getString("name");
			baseItem.type = result.getString("type");
			baseItem.packable = result.has("packable") ? result.getBoolean("packable") : true;
			baseItem.buy = result.has("buy") ? result.getInt("buy") : 0;
			baseItem.sell = result.has("sell") ? result.getInt("sell") : 0;
			
			return (T) baseItem;
		} catch (Exception e) {
			Logger.error("无法读取物品："+id,e);
			e.printStackTrace();
			return null;
		}
	}
	
	private void readProp(BaseItem item,JsonValue result){
		Map<String,String> replace = new HashMap<>();
		JsonValue props = result.get("prop");
		for(int i=0;i<props.size;i++){
			replace.put(props.get(i).name,props.getString(props.get(i).name));
		}
		
		item.effect.prop = replace;
		
	}
	
	/** 移除1个 <b><i>当前背包</i></b> 里的某个道具（根据ID）**/
	public boolean remove(int id){
		return remove(search(id),1);
	}
	
	/**
	 * 移除数个 <b><i>当前背包</i></b> 里的某个道具
	 * @param baseItem 道具实体类
	 * @param count 数量
	 * @return 操作是否成功
	 */
	public boolean remove(BaseItem baseItem,int count){
		if(baseItem==null)
			return false;
		if(baseItem.count-count < 0)
			return false;
		
		baseItem.count-=count;
		if(baseItem.count <= 0)
			RPG.global.items.remove(baseItem);
		return true;
	}
	
	/** 移除1个 <b><i>当前背包</i></b> 里的某个道具（根据实体类）**/
	public boolean remove(BaseItem baseItem){
		return remove(baseItem,1);
	}
	
	/** 移除数个 <b><i>当前背包</i></b> 里的某个道具（根据ID）**/
	public boolean remove(int id,int count){
		return remove(search(id),count);
	}
	
	public synchronized BaseItem search(int id){
		for(BaseItem baseItem:RPG.global.items)
			if(baseItem.id==id)
				return baseItem;
		return null;
	}
	
	/**
	 * 根据道具类型搜索出<b><i>当前背包</i></b> 里的一个或道具
	 * @param type 类型
	 * @return
	 */
	public ArrayList<BaseItem> search(String type){
		ArrayList<BaseItem> result = new ArrayList<BaseItem>();
		for(BaseItem baseItem:RPG.global.items)
			if(baseItem.type.equalsIgnoreCase(type))
				result.add(baseItem);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public <T> ArrayList<T> search(String type,Class<T> classType){
		return (ArrayList<T>)search(type);
	}
	
	private static void initReader(){
		if(reader==null)
			reader = new JsonReader();
	}
	
	/**
	 * 使用一个道具继承对象（道具 或 符卡 或 装备 等）
	 * @param id 要使用道具的ID
	 * @return 是否成功
	 */
	public boolean use(int id){
		return use(search(id));
	}
	
	/**
	 * 使用一个道具（道具 或 符卡 或 装备 等）
	 * @param baseItem 要使用的道具
	 * @return 是否成功
	 */
	public boolean use(BaseItem baseItem){
		if(baseItem==null)
			return false;
		
		
		if(baseItem instanceof Equipment){
			if(baseItem.user==null)
				return false;
			Equipment equip=(Equipment)baseItem;
			
			takeOff(equip);
			
			baseItem.user.equips.put(equip.equipType, equip);
			replace(baseItem.user, equip, true);//计算穿上装备后的Hero属性数值变化
			remove(equip);
		}else if(baseItem instanceof Item){
			Item item = (Item)baseItem;
			if(item.user==null || item.disable || item.effect==null)
				return false;
			Hero hero = item.user;
			
			hero.addProps(item.effect.prop);
			
			if(item.removeAble)
				remove(item);
		}
		
		baseItem.use();
		return true;
	}
	
	/**
	 * 从某个角色上脱下某件装备
	 * @param baseItem 新装备对比（不是要脱下的装备）（看不懂的话就别用这个方法……用下面那个方法）
	 * @return 是否成功脱下
	 */
	public boolean takeOff(BaseItem baseItem){
		if(!(baseItem instanceof Equipment))
			return false;
		return takeOff(baseItem.user,((Equipment)baseItem).equipType);
	}
	
	/**
	 * 从某个角色上脱下某件装备
	 * @param hero 角色
	 * @param equipType 装备的类型（如{@link Equipment.EQUIP_SHOES}）
	 * @return 是否成功脱下
	 */
	public boolean takeOff(Hero hero,String equipType){
		if(hero.equips.get(equipType)!=null){//脱下原先的装备（如果有）
			Equipment tmp=hero.equips.get(equipType);
			put(tmp);
			replace(hero, tmp, false);//计算脱下装备后的Hero属性数值变化
			
			hero.equips.remove(equipType);
			return true;
		}
		return false;
	}
	
	private static void replace(Hero hero,Equipment equip,boolean add){
		Map<String,String> prop = equip.effect.prop;
		for(String key:prop.keySet()){
			if(!add)
				if(prop.get(key).indexOf("-")>0)
					hero.addProp(key,prop.get(key));
				else
					hero.addProp(key,"-"+prop.get(key));
			else
				hero.addProp(key,prop.get(key));
		}
	}
	
	public Equipment getHeroEquip(Hero hero,String equipType){
		return hero.equips.get(equipType);
	}
	
}
