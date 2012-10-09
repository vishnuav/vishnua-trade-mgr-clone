/* ===========================================================
 * TradeManager : a application to trade strategies for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2011-2011, by Simon Allen and Contributors.
 *
 * Project Info:  org.trade
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Oracle, Inc.
 * in the United States and other countries.]
 *
 * (C) Copyright 2011-2011, by Simon Allen and Contributors.
 *
 * Original Author:  Simon Allen;
 * Contributor(s):   -;
 *
 * Changes
 * -------
 *
 */
package org.trade.persistent;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.OptimisticLockException;

import org.trade.core.dao.Aspect;
import org.trade.core.dao.AspectHome;
import org.trade.core.dao.Aspects;
import org.trade.core.util.CoreUtils;
import org.trade.core.util.TradingCalendar;
import org.trade.core.valuetype.Money;
import org.trade.dictionary.valuetype.Action;
import org.trade.dictionary.valuetype.OrderStatus;
import org.trade.dictionary.valuetype.Side;
import org.trade.dictionary.valuetype.TradestrategyStatus;
import org.trade.persistent.dao.Candle;
import org.trade.persistent.dao.CandleHome;
import org.trade.persistent.dao.Contract;
import org.trade.persistent.dao.ContractHome;
import org.trade.persistent.dao.Rule;
import org.trade.persistent.dao.RuleHome;
import org.trade.persistent.dao.Strategy;
import org.trade.persistent.dao.StrategyHome;
import org.trade.persistent.dao.Trade;
import org.trade.persistent.dao.TradeAccount;
import org.trade.persistent.dao.TradeAccountHome;
import org.trade.persistent.dao.TradeHome;
import org.trade.persistent.dao.TradeOrder;
import org.trade.persistent.dao.TradeOrderHome;
import org.trade.persistent.dao.TradeOrderfill;
import org.trade.persistent.dao.TradeOrderfillHome;
import org.trade.persistent.dao.TradelogHome;
import org.trade.persistent.dao.TradelogReport;
import org.trade.persistent.dao.Tradestrategy;
import org.trade.persistent.dao.TradestrategyHome;
import org.trade.persistent.dao.Tradingday;
import org.trade.persistent.dao.TradingdayHome;
import org.trade.persistent.dao.Tradingdays;
import org.trade.strategy.data.CandleSeries;
import org.trade.strategy.data.candle.CandleItem;

/**
 */
public class TradePersistentModel implements PersistentModel {

	private ContractHome m_contractHome = null;
	private StrategyHome m_strategyHome = null;
	private TradingdayHome m_tradingdayHome = null;
	private TradeOrderHome m_tradeOrderHome = null;
	private TradeOrderfillHome m_tradeOrderfillHome = null;
	private TradeHome m_tradeHome = null;
	private TradelogHome m_tradelogHome = null;
	private TradeAccountHome m_tradeAccountHome = null;
	private TradestrategyHome m_tradestrategyHome = null;
	private CandleHome m_candleHome = null;
	private AspectHome m_aspectHome = null;
	private RuleHome m_ruleHome = null;

	private static final int SCALE = 5;

	public TradePersistentModel() {
		m_contractHome = new ContractHome();
		m_strategyHome = new StrategyHome();
		m_tradingdayHome = new TradingdayHome();
		m_tradeOrderHome = new TradeOrderHome();
		m_tradeOrderfillHome = new TradeOrderfillHome();
		m_tradeHome = new TradeHome();
		m_tradelogHome = new TradelogHome();
		m_tradeAccountHome = new TradeAccountHome();
		m_tradestrategyHome = new TradestrategyHome();
		m_candleHome = new CandleHome();
		m_aspectHome = new AspectHome();
		m_ruleHome = new RuleHome();
	}

	/**
	 * Method findTradelogReport.
	 * @param tradeAccount TradeAccount
	 * @param start Date
	 * @param end Date
	 * @param filter boolean
	 * @return TradelogReport
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradelogReport(TradeAccount, Date, Date, boolean)
	 */
	public TradelogReport findTradelogReport(TradeAccount tradeAccount,
			Date start, Date end, boolean filter)
			throws PersistentModelException {
		return m_tradelogHome.findByTradelogReport(tradeAccount, start, end,
				filter);
	}

	/**
	 * Method findTradeAccountById.
	 * @param id Integer
	 * @return TradeAccount
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradeAccountById(Integer)
	 */
	public TradeAccount findTradeAccountById(Integer id)
			throws PersistentModelException {
		TradeAccount instance = m_tradeAccountHome.findById(id);
		if (null == instance)
			throw new PersistentModelException(
					"TradeAccount not found for id: " + id);
		return instance;
	}

	/**
	 * Method findTradeAccountByNumber.
	 * @param accountNumber String
	 * @return TradeAccount
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradeAccountByNumber(String)
	 */
	public TradeAccount findTradeAccountByNumber(String accountNumber)
			throws PersistentModelException {
		return m_tradeAccountHome.findByAccountNumber(accountNumber);
	}

	/**
	 * Method findTradingdayById.
	 * @param id Integer
	 * @return Tradingday
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradingdayById(Integer)
	 */
	public Tradingday findTradingdayById(Integer id)
			throws PersistentModelException {
		Tradingday instance = m_tradingdayHome.findTradingdayById(id);
		if (null == instance)
			throw new PersistentModelException("Tradingday not found for id: "
					+ id);
		return instance;
	}

	/**
	 * Method findTradingdayByOpenDate.
	 * @param date Date
	 * @return Tradingday
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradingdayByOpenDate(Date)
	 */
	public Tradingday findTradingdayByOpenDate(Date date)
			throws PersistentModelException {
		return m_tradingdayHome.findByOpen(date);
	}

	/**
	 * Method findContractById.
	 * @param id Integer
	 * @return Contract
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findContractById(Integer)
	 */
	public Contract findContractById(Integer id)
			throws PersistentModelException {
		Contract instance = m_contractHome.findById(id);
		if (null == instance)
			throw new PersistentModelException("Contract not found for id: "
					+ id);
		return instance;
	}

	/**
	 * Method findContractByUniqueKey.
	 * @param SECType String
	 * @param symbol String
	 * @param exchange String
	 * @param currency String
	 * @return Contract
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findContractByUniqueKey(String, String, String, String)
	 */
	public Contract findContractByUniqueKey(String SECType, String symbol,
			String exchange, String currency) throws PersistentModelException {
		return m_contractHome.findByUniqueKey(SECType, symbol, exchange,
				currency);
	}

	/**
	 * Method findTradestrategyById.
	 * @param tradestrategy Tradestrategy
	 * @return Tradestrategy
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradestrategyById(Tradestrategy)
	 */
	public Tradestrategy findTradestrategyById(Tradestrategy tradestrategy)
			throws PersistentModelException {
		if (null == tradestrategy.getIdTradeStrategy())
			throw new PersistentModelException(
					"Please save Tradestrategy for symbol: "
							+ tradestrategy.getContract().getSymbol());

		Tradestrategy instance = m_tradestrategyHome.findById(tradestrategy
				.getIdTradeStrategy());
		if (null == instance)
			throw new PersistentModelException(
					"Tradestrategy not found for id: "
							+ tradestrategy.getIdTradeStrategy());

		instance.setDatasetContainer(tradestrategy.getDatasetContainer());
		return instance;
	}

	/**
	 * Method findTradestrategyById.
	 * @param id Integer
	 * @return Tradestrategy
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradestrategyById(Integer)
	 */
	public Tradestrategy findTradestrategyById(Integer id)
			throws PersistentModelException {
		Tradestrategy instance = m_tradestrategyHome.findById(id);
		if (null == instance)
			throw new PersistentModelException(
					"Tradestrategy not found for id: " + id);
		return instance;
	}

	/**
	 * Method findTradeById.
	 * @param id Integer
	 * @return Trade
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradeById(Integer)
	 */
	public Trade findTradeById(Integer id) throws PersistentModelException {
		Trade instance = m_tradeHome.findById(id);
		if (null == instance)
			throw new PersistentModelException("Trade not found for id: " + id);
		return instance;
	}

	/**
	 * Method findOpenTradeByTradestrategyId.
	 * @param id Integer
	 * @return Trade
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findOpenTradeByTradestrategyId(Integer)
	 */
	public Trade findOpenTradeByTradestrategyId(Integer id)
			throws PersistentModelException {
		return m_tradeHome.findOpenTradeByTradestrategyId(id);
	}

	/**
	 * Method findTradesByTradestrategyId.
	 * @param idTradestrategy Integer
	 * @return List<Trade>
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradesByTradestrategyId(Integer)
	 */
	public List<Trade> findTradesByTradestrategyId(Integer idTradestrategy)
			throws PersistentModelException {
		return m_tradeHome.findByTradestrategyId(idTradestrategy);
	}

	/**
	 * Method findAllTradestrategies.
	 * @return List<Tradestrategy>
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findAllTradestrategies()
	 */
	public List<Tradestrategy> findAllTradestrategies()
			throws PersistentModelException {
		return m_tradestrategyHome.findAll();

	}

	/**
	 * Method findTradestrategyByUniqueKeys.
	 * @param open Date
	 * @param strategy String
	 * @param idContract Integer
	 * @param accountNumber String
	 * @return Tradestrategy
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradestrategyByUniqueKeys(Date, String, Integer, String)
	 */
	public Tradestrategy findTradestrategyByUniqueKeys(Date open,
			String strategy, Integer idContract, String accountNumber)
			throws PersistentModelException {
		return m_tradestrategyHome.findTradestrategyByUniqueKeys(open,
				strategy, idContract, accountNumber);
	}

	/**
	 * Method removeTradingdayTrades.
	 * @param transientInstance Tradingday
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#removeTradingdayTrades(Tradingday)
	 */
	public void removeTradingdayTrades(Tradingday transientInstance)
			throws PersistentModelException {
		for (Tradestrategy tradestrategy : transientInstance
				.getTradestrategies()) {
			this.removeTradestrategyTrades(tradestrategy);
		}
	}

	/**
	 * Method removeTradestrategyTrades.
	 * @param transientInstance Tradestrategy
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#removeTradestrategyTrades(Tradestrategy)
	 */
	public void removeTradestrategyTrades(Tradestrategy transientInstance)
			throws PersistentModelException {

		try {

			for (Trade trade : transientInstance.getTrades()) {
				/*
				 * Get the latest version of the trade to delete.
				 */
				if (null != trade.getIdTrade()) {
					Aspect instance = m_aspectHome.findById(trade);
					m_aspectHome.remove(instance);
				}
			}
			transientInstance.setStatus(null);
			transientInstance.getTrades().clear();
			m_aspectHome.persist(transientInstance);

		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error removing Tradestrategy Trades. Please refresh before save.");

		} catch (Exception ex) {
			throw new PersistentModelException(
					"Error removing Tradestrategy Trades: "
							+ transientInstance.getContract().getSymbol() + " "
							+ ex.getMessage());
		}
	}

	/**
	 * Method findTradeOrderByKey.
	 * @param orderKey Integer
	 * @return TradeOrder
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradeOrderByKey(Integer)
	 */
	public TradeOrder findTradeOrderByKey(Integer orderKey)
			throws PersistentModelException {
		return m_tradeOrderHome.findTradeOrderByKey(orderKey);
	}

	/**
	 * Method findTradeOrderfillByExecId.
	 * @param execId String
	 * @return TradeOrderfill
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradeOrderfillByExecId(String)
	 */
	public TradeOrderfill findTradeOrderfillByExecId(String execId)
			throws PersistentModelException {
		return m_tradeOrderfillHome.findOrderFillByExecId(execId);
	}

	/**
	 * Method findTradeOrderByMaxKey.
	 * @return Integer
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradeOrderByMaxKey()
	 */
	public Integer findTradeOrderByMaxKey() throws PersistentModelException {
		return m_tradeOrderHome.findTradeOrderByMaxKey();
	}

	/**
	 * Method findTradingdaysByDateRange.
	 * @param startDate Date
	 * @param endDate Date
	 * @return Tradingdays
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findTradingdaysByDateRange(Date, Date)
	 */
	public Tradingdays findTradingdaysByDateRange(Date startDate, Date endDate)
			throws PersistentModelException {
		return m_tradingdayHome.findTradingdaysByDateRange(startDate, endDate);
	}

	/**
	 * Method findCandlesByContractAndDateRange.
	 * @param idContract Integer
	 * @param startDate Date
	 * @param endDate Date
	 * @return List<Candle>
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findCandlesByContractAndDateRange(Integer, Date, Date)
	 */
	public List<Candle> findCandlesByContractAndDateRange(Integer idContract,
			Date startDate, Date endDate) throws PersistentModelException {
		return m_candleHome.findCandlesByTradingdayContract(idContract,
				startDate, endDate);
	}

	/**
	 * Method findCandleCount.
	 * @param idTradingday Integer
	 * @param idContract Integer
	 * @return Long
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findCandleCount(Integer, Integer)
	 */
	public Long findCandleCount(Integer idTradingday, Integer idContract)
			throws PersistentModelException {
		return m_candleHome.findCandleCount(idTradingday, idContract);
	}

	/**
	 * Method persistTradingday.
	 * @param transientInstance Tradingday
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistTradingday(Tradingday)
	 */
	public void persistTradingday(Tradingday transientInstance)
			throws PersistentModelException {

		try {
			if (transientInstance.isDirty()) {
				m_tradingdayHome.persist(transientInstance);
			}
		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Tradingday please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving Tradingday: "
					+ transientInstance.getOpen() + " Msg: " + e.getMessage());
		}
	}

	/**
	 * Method persistContract.
	 * @param transientInstance Contract
	 * @return Contract
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistContract(Contract)
	 */
	public Contract persistContract(Contract transientInstance)
			throws PersistentModelException {

		try {
			if (null == transientInstance.getIdContract()) {
				Contract currentContract = m_contractHome.findByUniqueKey(
						transientInstance.getSecType(),
						transientInstance.getSymbol(),
						transientInstance.getExchange(),
						transientInstance.getCurrency());
				if (null != currentContract) {
					transientInstance.setIdContract(currentContract
							.getIdContract());
					transientInstance.setVersion(currentContract.getVersion());
				}
			}

			return (Contract) m_aspectHome.persist(transientInstance);
		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Contract please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving Contract: "
					+ transientInstance.getSymbol() + " Msg: " + e.getMessage());
		}
	}

	/**
	 * Method resetDefaultTradeAccount.
	 * @param transientInstance TradeAccount
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#resetDefaultTradeAccount(TradeAccount)
	 */
	public void resetDefaultTradeAccount(TradeAccount transientInstance)
			throws PersistentModelException {

		try {
			m_tradeAccountHome.resetDefaultTradeAccount(transientInstance);

		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Contract please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving Trade Account: "
					+ transientInstance.getAccountNumber() + " Msg: "
					+ e.getMessage());
		}
	}

	/**
	 * Method persistCandleSeries.
	 * @param candleSeries CandleSeries
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistCandleSeries(CandleSeries)
	 */
	public void persistCandleSeries(CandleSeries candleSeries)
			throws PersistentModelException {
		try {
			/*
			 * This can happen when an indicator is a contract that has never
			 * been used.
			 */
			if (null == candleSeries.getContract().getIdContract()) {
				Contract contract = this.persistContract(candleSeries
						.getContract());
				candleSeries.getContract().setIdContract(
						contract.getIdContract());
				candleSeries.getContract().setVersion(contract.getVersion());
			}
			m_candleHome.persistCandleSeries(candleSeries);
		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving CandleSeries please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving CandleSeries: "
					+ candleSeries.getDescription() + " Msg: " + e.getMessage());
		}
	}

	/**
	 * Method persistCandleItem.
	 * @param candleItem CandleItem
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistCandleItem(CandleItem)
	 */
	public void persistCandleItem(CandleItem candleItem)
			throws PersistentModelException {
		try {
			synchronized (candleItem) {
				if (null == candleItem.getCandle().getTradingday()) {
					Date open = TradingCalendar.getBusinessDayStart(candleItem
							.getPeriod().getStart());
					Tradingday tradingday = this.findTradingdayByOpenDate(open);
					/*
					 * This could be the previous day so we need to add a
					 * trading day.
					 */
					if (null == tradingday) {
						tradingday = Tradingday.newInstance(open);
						tradingday = (Tradingday) m_aspectHome
								.persist(tradingday);
					}
					candleItem.getCandle().setTradingday(tradingday);
				}
				if (null == candleItem.getCandle().getIdCandle()) {
					Candle currCandle = m_candleHome.findByUniqueKey(candleItem
							.getCandle().getTradingday().getIdTradingDay(),
							candleItem.getCandle().getContract()
									.getIdContract(), candleItem.getCandle()
									.getStartPeriod(), candleItem.getCandle()
									.getEndPeriod());
					/*
					 * Candle exists set the id and version so we can merge the
					 * incoming candle.
					 */
					if (null != currCandle) {
						candleItem.getCandle().setIdCandle(
								currCandle.getIdCandle());
						candleItem.getCandle().setVersion(
								currCandle.getVersion());
					}
				}
				m_aspectHome.persist(candleItem.getCandle());
			}
		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Candle please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving CandleItem: "
					+ candleItem.getOpen() + " Msg: " + e.getMessage());
		}
	}

	/**
	 * Method persistTradestrategy.
	 * @param transientInstance Tradestrategy
	 * @return Tradestrategy
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistTradestrategy(Tradestrategy)
	 */
	public Tradestrategy persistTradestrategy(Tradestrategy transientInstance)
			throws PersistentModelException {

		try {
			return (Tradestrategy) m_aspectHome.persist(transientInstance);

		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Tradestrategy please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException(" Error saving Tradestrategy: "
					+ transientInstance.getContract().getSymbol() + " Msg: "
					+ e.getMessage());
		}
	}

	/**
	 * Method persistTradeOrder.
	 * @param tradeOrder TradeOrder
	 * @return TradeOrder
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistTradeOrder(TradeOrder)
	 */
	public synchronized TradeOrder persistTradeOrder(TradeOrder tradeOrder)
			throws PersistentModelException {
		try {

			if (null == tradeOrder.getOrderKey()) {
				throw new PersistentModelException("Order key cannot be null.");
			}
			Integer idTradeStrategy = null;
			Trade trade = null;
			/*
			 * If idTrade is null then this is the first order for the trade.
			 */
			if (null == tradeOrder.getTrade().getIdTrade()) {
				idTradeStrategy = tradeOrder.getTrade().getTradestrategy()
						.getIdTradeStrategy();
				tradeOrder.getTrade().setSide(
						(Action.BUY.equals(tradeOrder.getAction()) ? Side.BOT
								: Side.SLD));
				tradeOrder.setIsOpenPosition(true);
				trade = (Trade) this.persistAspect(tradeOrder.getTrade());
				tradeOrder.setTrade(trade);
			} else {
				trade = this.findTradeById(tradeOrder.getTrade().getIdTrade());
				idTradeStrategy = trade.getTradestrategyId()
						.getIdTradeStrategy();
			}
			if (null == tradeOrder.getIdTradeOrder()) {
				trade.addTradeOrder(tradeOrder);
			}
			/*
			 * This is a new order set the status to UNSUBMIT
			 */
			if (null == tradeOrder.getIdTradeOrder()
					&& null == tradeOrder.getFilledQuantity()) {
				tradeOrder.setStatus(OrderStatus.UNSUBMIT);
			}

			if (!tradeOrder.getIsFilled()
					&& CoreUtils.nullSafeIntegerComparator(
							tradeOrder.getQuantity(),
							tradeOrder.getFilledQuantity()) == 0) {
				tradeOrder.setIsFilled(true);
				tradeOrder.setStatus(OrderStatus.FILLED);
			}

			boolean allOrdersCancelled = true;
			int totalOpenQuantity = 0;
			int totalFilledQuantity = 0;
			double totalCommission = 0;
			double totalFilledValue = 0;

			for (TradeOrder order : trade.getTradeOrders()) {

				if (order.getOrderKey().equals(tradeOrder.getOrderKey())) {
					order = tradeOrder;
				}

				/*
				 * If all orders are cancelled and not filled then we need to
				 * update the tradestrategy status to cancelled.
				 */
				if (!OrderStatus.CANCELLED.equals(order.getStatus())) {
					allOrdersCancelled = false;
				}

				if (null != order.getFilledQuantity()) {
					int buySellMultiplier = 1;
					if (Action.BUY.equals(order.getAction())) {
						buySellMultiplier = -1;
					}
					totalFilledQuantity = totalFilledQuantity
							+ order.getFilledQuantity();
					totalOpenQuantity = totalOpenQuantity
							+ (order.getFilledQuantity() * buySellMultiplier);
					if (null != order.getAverageFilledPrice()) {
						totalFilledValue = totalFilledValue
								+ (order.getAverageFilledPrice().doubleValue()
										* order.getFilledQuantity() * buySellMultiplier);
					}
					if (null != order.getCommission()) {
						totalCommission = totalCommission
								+ order.getCommission().doubleValue();
					}
				}
			}
			/*
			 * totalFilledQuantity has changed for the trade update the trade
			 * values.
			 */
			if (totalFilledQuantity > 0
					&& CoreUtils.nullSafeIntegerComparator(totalFilledQuantity,
							trade.getTotalQuantity()) != 0) {
				trade.setTotalQuantity(totalFilledQuantity);
				trade.setOpenQuantity(totalOpenQuantity);
				Money filledValue = new Money(totalFilledValue);
				trade.setTotalValue(
						filledValue.getBigDecimalValue());
				BigDecimal avgFillPrice = new BigDecimal(totalFilledValue
						/ (totalFilledQuantity / 2));
				avgFillPrice.setScale(SCALE, BigDecimal.ROUND_HALF_EVEN);
				trade.setAveragePrice(avgFillPrice);
				Money comms = new Money(totalCommission);
				trade.setTotalCommission(
						comms.getBigDecimalValue());
				trade.setProfitLoss(
						filledValue.getBigDecimalValue());

				if (!trade.getIsOpen()) {
					if (totalOpenQuantity != 0) {
						trade.setIsOpen(true);
						Tradestrategy tradestrategy = this
								.findTradestrategyById(idTradeStrategy);
						tradestrategy.setStatus(TradestrategyStatus.OPEN);
						this.persistTradestrategy(tradestrategy);
					}
				} else {
					if (totalOpenQuantity == 0) {
						trade.setIsOpen(false);
						Tradestrategy tradestrategy = this
								.findTradestrategyById(idTradeStrategy);
						tradestrategy.setStatus(TradestrategyStatus.CLOSED);
						this.persistTradestrategy(tradestrategy);
					}
				}
				trade = (Trade) this.persistAspect(trade);

			} else {
				if (allOrdersCancelled) {
					Tradestrategy tradestrategy = this
							.findTradestrategyById(idTradeStrategy);
					if (null == tradestrategy.getStatus()) {
						tradestrategy.setStatus(TradestrategyStatus.CANCELLED);
						this.persistTradestrategy(tradestrategy);
					}
				}
			}
			
			return (TradeOrder) this.persistAspect(tradeOrder);
			
		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving TradeOrder please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving TradeOrder: "
					+ tradeOrder.getOrderKey() + " Msg: " + e.getMessage());
		}
	}

	/**
	 * Method persistTradeOrderfill.
	 * @param tradeOrder TradeOrder
	 * @return TradeOrder
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistTradeOrderfill(TradeOrder)
	 */
	public synchronized TradeOrder persistTradeOrderfill(TradeOrder tradeOrder)
			throws PersistentModelException {
		try {

			Date filledDate = null;
			double filledValue = 0;
			int filledQuantity = 0;
			for (TradeOrderfill tradeOrderfill : tradeOrder
					.getTradeOrderfills()) {

				filledQuantity = filledQuantity + tradeOrderfill.getQuantity();
				filledValue = filledValue
						+ (tradeOrderfill.getAveragePrice().doubleValue() * tradeOrderfill
								.getQuantity());
				filledDate = tradeOrderfill.getTime();
			}

			if (filledQuantity > 0) {
				if (filledQuantity == tradeOrder.getQuantity())
					tradeOrder.setIsFilled(true);
				BigDecimal avgFillPrice = new BigDecimal(filledValue
						/ filledQuantity);
				avgFillPrice.setScale(SCALE, BigDecimal.ROUND_HALF_EVEN);
				tradeOrder.setAverageFilledPrice(avgFillPrice);
				tradeOrder.setFilledQuantity(filledQuantity);
				tradeOrder.setFilledDate(filledDate);
			}

			return persistTradeOrder(tradeOrder);

		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving TradeOrder please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving TradeOrder: "
					+ tradeOrder.getOrderKey() + " Msg: " + e.getMessage());
		}
	}

	/**
	 * Method persistTrade.
	 * @param transientInstance Trade
	 * @return Trade
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistTrade(Trade)
	 */
	public Trade persistTrade(Trade transientInstance)
			throws PersistentModelException {

		try {
			return (Trade) m_aspectHome.persist(transientInstance);

		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Trade please refresh before save.");
		} catch (Exception e) {

			throw new PersistentModelException("Error saving Trade: "
					+ transientInstance.getTradestrategy().getContract()
							.getSymbol() + " Msg: " + e.getMessage());
		}
	}

	/**
	 * Method findRuleById.
	 * @param id Integer
	 * @return Rule
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findRuleById(Integer)
	 */
	public Rule findRuleById(Integer id) throws PersistentModelException {
		Rule instance = m_ruleHome.findById(id);
		if (null == instance)
			throw new PersistentModelException("Rule not found for Id: " + id);
		return instance;
	}

	/**
	 * Method findRuleByMaxVersion.
	 * @param strategy Strategy
	 * @return Integer
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findRuleByMaxVersion(Strategy)
	 */
	public Integer findRuleByMaxVersion(Strategy strategy)
			throws PersistentModelException {
		return m_ruleHome.findByMaxVersion(strategy);
	}

	/**
	 * Method findStrategyById.
	 * @param id Integer
	 * @return Strategy
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findStrategyById(Integer)
	 */
	public Strategy findStrategyById(Integer id)
			throws PersistentModelException {
		Strategy instance = m_strategyHome.findById(id);
		if (null == instance)
			throw new PersistentModelException("Strategy not found for Id: "
					+ id);
		return instance;
	}

	/**
	 * Method findStrategyByName.
	 * @param name String
	 * @return Strategy
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findStrategyByName(String)
	 */
	public Strategy findStrategyByName(String name)
			throws PersistentModelException {
		return m_strategyHome.findByName(name);
	}

	/**
	 * Method findStrategies.
	 * @return List<Strategy>
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findStrategies()
	 */
	public List<Strategy> findStrategies() throws PersistentModelException {
		return m_strategyHome.findAll();
	}

	/**
	 * Method persistRule.
	 * @param transientInstance Rule
	 * @return Rule
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistRule(Rule)
	 */
	public Rule persistRule(Rule transientInstance)
			throws PersistentModelException {
		try {
			return (Rule) m_aspectHome.persist(transientInstance);
		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Aspect please refresh before save.");
		} catch (Exception ex) {
			throw new PersistentModelException("Error saving Aspect: "
					+ ex.getMessage());
		}
	}

	/**
	 * Method findAspectsByClassName.
	 * @param aspectClassName String
	 * @return Aspects
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findAspectsByClassName(String)
	 */
	public Aspects findAspectsByClassName(String aspectClassName)
			throws PersistentModelException {
		try {

			if ("org.trade.persistent.dao.Strategy".equals(aspectClassName)) {
				/*
				 * TODO Eager does not work on the relationship Strategy ->
				 * IndicatorSeries so hack needed.
				 */
				List<Strategy> strategies = m_strategyHome.findAll();
				Aspects aspects = new Aspects();
				for (Object item : strategies) {
					aspects.add((Aspect) item);
				}
				return aspects;
			} else {
				return m_aspectHome.findByClassName(aspectClassName);
			}

		} catch (Exception ex) {
			throw new PersistentModelException("Error finding Aspects: "
					+ ex.getMessage());
		}
	}

	/**
	 * Method findAspectsByClassNameFieldName.
	 * @param className String
	 * @param fieldname String
	 * @param value String
	 * @return Aspects
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findAspectsByClassNameFieldName(String, String, String)
	 */
	public Aspects findAspectsByClassNameFieldName(String className,
			String fieldname, String value) throws PersistentModelException {
		try {
			return m_aspectHome.findByClassNameFieldName(className, fieldname,
					value);
		} catch (Exception ex) {
			throw new PersistentModelException("Error finding Aspects: "
					+ ex.getMessage());
		}
	}

	/**
	 * Method findAspectById.
	 * @param transientInstance Aspect
	 * @return Aspect
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#findAspectById(Aspect)
	 */
	public Aspect findAspectById(Aspect transientInstance)
			throws PersistentModelException {
		Aspect instance = m_aspectHome.findById(transientInstance);
		if (null == instance)
			throw new PersistentModelException("Aspect not found for Id: "
					+ transientInstance.getId());
		return instance;
	}

	/**
	 * Method persistAspect.
	 * @param transientInstance Aspect
	 * @return Aspect
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#persistAspect(Aspect)
	 */
	public Aspect persistAspect(Aspect transientInstance)
			throws PersistentModelException {
		try {
			return m_aspectHome.persist(transientInstance);
		} catch (OptimisticLockException ex1) {
			throw new PersistentModelException(
					"Error saving Aspect please refresh before save.");
		} catch (Exception e) {
			throw new PersistentModelException("Error saving Aspect: "
					+ e.getMessage());
		}
	}

	/**
	 * Method removeAspect.
	 * @param transientInstance Aspect
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#removeAspect(Aspect)
	 */
	public void removeAspect(Aspect transientInstance)
			throws PersistentModelException {
		m_aspectHome.remove(transientInstance);
	}

	/**
	 * Method removeRule.
	 * @param transientInstance Rule
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#removeRule(Rule)
	 */
	public void removeRule(Rule transientInstance)
			throws PersistentModelException {
		m_aspectHome.remove(transientInstance);
	}

	/**
	 * Method reassignStrategy.
	 * @param fromStrategy Strategy
	 * @param toStrategy Strategy
	 * @param tradingday Tradingday
	 * @throws PersistentModelException
	 * @see org.trade.persistent.PersistentModel#reassignStrategy(Strategy, Strategy, Tradingday)
	 */
	public void reassignStrategy(Strategy fromStrategy, Strategy toStrategy,
			Tradingday tradingday) throws PersistentModelException {

		try {
			for (Tradestrategy item : tradingday.getTradestrategies()) {
				if (item.getStrategy().getIdStrategy()
						.equals(fromStrategy.getIdStrategy())) {
					item.setStrategy(toStrategy);
					item.setDirty(true);
					item.setDatasetContainer(null);
					m_aspectHome.persist(item);
				}
			}

		} catch (Exception ex) {
			throw new PersistentModelException("Error reassign Strategy: "
					+ ex.getMessage());
		}
	}
}