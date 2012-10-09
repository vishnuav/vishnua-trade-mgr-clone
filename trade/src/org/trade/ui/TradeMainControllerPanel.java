/* ===========================================================
 * TradeManager : An application to trade strategies for the Java(tm) platform
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
package org.trade.ui;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trade.broker.BackTestBroker;
import org.trade.broker.BrokerChangeListener;
import org.trade.broker.BrokerModel;
import org.trade.broker.BrokerModelException;
import org.trade.core.factory.ClassFactory;
import org.trade.core.lookup.DBTableLookupServiceProvider;
import org.trade.core.properties.ConfigProperties;
import org.trade.core.util.DynamicCode;
import org.trade.core.util.TradingCalendar;
import org.trade.dictionary.valuetype.Currency;
import org.trade.dictionary.valuetype.OrderStatus;
import org.trade.persistent.PersistentModel;
import org.trade.persistent.PersistentModelException;
import org.trade.persistent.dao.Candle;
import org.trade.persistent.dao.Strategy;
import org.trade.persistent.dao.Trade;
import org.trade.persistent.dao.TradeAccount;
import org.trade.persistent.dao.TradeOrder;
import org.trade.persistent.dao.Tradestrategy;
import org.trade.persistent.dao.Tradingday;
import org.trade.persistent.dao.Tradingdays;
import org.trade.strategy.StrategyChangeListener;
import org.trade.strategy.StrategyRule;
import org.trade.strategy.StrategyRuleException;
import org.trade.strategy.data.CandleDataset;
import org.trade.strategy.data.CandleSeries;
import org.trade.strategy.data.IndicatorSeries;
import org.trade.ui.base.BasePanel;
import org.trade.ui.base.ConnectionPane;
import org.trade.ui.base.ComponentPrintService;
import org.trade.ui.base.TabbedAppPanel;
import org.trade.ui.base.TextDialog;
import org.trade.ui.configuration.ConfigurationPanel;
import org.trade.ui.contract.ContractPanel;
import org.trade.ui.portfolio.PortfolioPanel;
import org.trade.ui.strategy.StrategyPanel;
import org.trade.ui.tradingday.TradingdayPanel;

/**
 * The apps main contraoller.
 */
public class TradeMainControllerPanel extends TabbedAppPanel implements
		BrokerChangeListener, StrategyChangeListener {

	private static final long serialVersionUID = -7717664255656430982L;

	private final static Logger _log = LoggerFactory
			.getLogger(TradeMainControllerPanel.class);

	public static String title = null;
	public static String version = null;
	public static String date = null;
	private TradeMainPanelMenu m_menuBar = null;
	protected static TradeMainControllerPanel m_instance = null;

	private static Tradingdays m_tradingdays = null;

	private BrokerModel m_brokerModel = null;
	private PersistentModel m_tradePersistentModel = null;
	private BrokerDataRequestProgressMonitor brokerDataRequestProgressMonitor = null;
	private static final ConcurrentHashMap<String, StrategyRule> m_strategyWorkers = new ConcurrentHashMap<String, StrategyRule>();
	private static final ConcurrentHashMap<Integer, Tradestrategy> m_indicatorTradestrategy = new ConcurrentHashMap<Integer, Tradestrategy>();

	private TradingdayPanel tradingdayPanel = null;
	private ContractPanel contractPanel = null;
	private ConfigurationPanel configurationPanel = null;
	private StrategyPanel strategyPanel = null;
	private PortfolioPanel portfolioPanel = null;
	private DynamicCode dynacode = null;

	/**
	 * The main application controller which interacts between the view and the
	 * applications underlying models. This controller also listens to events
	 * from the broker model.
	 * <p>
	 * 
	 * @param frame
	 *            the main application Frame.
	 * 
	 */

	public TradeMainControllerPanel(Frame frame) {
		super(frame);
		try {
			m_menuBar = new TradeMainPanelMenu(this);
			setMenu(m_menuBar);
			/* This is always true as main panel needs to receive all events */
			setSelected(true);
			title = ConfigProperties.getPropAsString("component.name.base");
			version = ConfigProperties
					.getPropAsString("component.name.version");
			date = ConfigProperties.getPropAsString("component.name.date");
			m_tradePersistentModel = (PersistentModel) ClassFactory
					.getServiceForInterface(PersistentModel._persistentModel,
							this);
			m_tradingdays = new Tradingdays();
			m_tradingdays.add(Tradingday.newInstance(TradingCalendar
					.getMostRecentTradingDay(new Date())));
			String strategyDir = ConfigProperties
					.getPropAsString("trade.strategy.default.dir");
			dynacode = new DynamicCode();
			dynacode.addSourceDir(new File(strategyDir));
			simulatedMode(true);
		} catch (Exception ex) {
			this.setErrorMessage("Error During Initialization.",
					ex.getMessage(), ex);
			System.exit(0);
		}
	}

	/**
	 * Constructs a new Trading tab that contains all information related to the
	 * tradeingday i.e. which strategy to trade, contract information whether to
	 * trade. This is the tab used to load contracts and decide how to trade
	 * them.
	 * 
	 */

	public void openTradingdayView() {
		tradingdayPanel = new TradingdayPanel(m_tradingdays, this,
				m_tradePersistentModel, m_strategyWorkers);
		getMenu().addMessageListener(tradingdayPanel);
		this.addTab("Tradingday", tradingdayPanel);
	}

	/**
	 * Constructs a new Contract tab that contains all information related to
	 * the Tradestrategy i.e. charts, Orders for a particular trading day.
	 * 
	 */

	public void openContractView() {
		contractPanel = new ContractPanel(m_tradingdays, this,
				m_tradePersistentModel);
		getMenu().addMessageListener(contractPanel);
		this.addTab("Contract Details", contractPanel);
	}

	/**
	 * Constructs a new Portfolio tab that contains all information related to a
	 * portfolio. This tab allows you to see the results of trading activity. It
	 * records the summary information for each month i.e. Batting avg, Simple
	 * Sharpe ratio and P/L information.
	 * 
	 */

	public void openPortfolioView() {
		portfolioPanel = new PortfolioPanel(this, m_tradePersistentModel);
		getMenu().addMessageListener(portfolioPanel);
		this.addTab("Portfolio", portfolioPanel);
	}

	/**
	 * Constructs a new Configuration tab that contains all information related
	 * to a portfolio. This tab allows you to see the results of trading
	 * activity. It records the summary information for each month i.e. Batting
	 * avg, Simple Sharpe ratio and P/L information.
	 * 
	 */

	public void openConfigurationView() {
		configurationPanel = new ConfigurationPanel(m_tradePersistentModel);
		getMenu().addMessageListener(configurationPanel);
		this.addTab("Configuration", configurationPanel);
	}

	/**
	 * Constructs a new Strategy tab that contains all information related to a
	 * Strategy. This tab allows you to see the java code of a strategy. It will
	 * be replaced in the future with Drools and this will be where you can edit
	 * the strategies and deploy them.
	 * 
	 */

	public void openStrategyView() {
		strategyPanel = new StrategyPanel(m_tradePersistentModel);
		getMenu().addMessageListener(strategyPanel);
		this.addTab("Strategies", strategyPanel);
	}

	/**
	 * This is fired when the menu item to open a file is fired.
	 * 
	 */

	public void doOpen() {

	}

	/**
	 * This is fired from the Tradingday Tab when the Request Executions button
	 * is pressed. This should be used to fetch orders that have executed at the
	 * broker while the system was down.
	 * 
	 * @param tradestrategy
	 *            the Tradestrategy for which you are requesting trade
	 *            executions
	 * 
	 */

	public void doFetch(final Tradestrategy tradestrategy) {
		try {
			if (null != tradestrategy.getIdTradeStrategy()) {
				m_brokerModel.onReqExecutions(tradestrategy);
			}
		} catch (BrokerModelException ex) {
			setErrorMessage("Error getting executions.", ex.getMessage(), ex);
		}
	}

	/**
	 * This is fired from the Tradingday Tab when the Request Executions button
	 * is pressed. This should be used to fetch orders that have executed at the
	 * broker while the system was down.
	 * 
	 * @param tradestrategy
	 *            the Tradestrategy for which you are requesting trade
	 *            executions
	 * 
	 */

	public void doMarketData(final Tradestrategy tradestrategy) {
		try {
			_log.info("doMarketData" + tradestrategy.getIdTradeStrategy());
			if (null != tradestrategy.getIdTradeStrategy()) {
				m_brokerModel.onReqMarketData(tradestrategy);
			}
		} catch (BrokerModelException ex) {
			setErrorMessage("Error subscribing to market data.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This is fired from the main menu when the Broker data button is pressed.
	 * This will run the Strategy for all the tradingdays.
	 * 
	 * 
	 */

	public void doData() {
		if (m_tradingdays.isDirty()) {
			this.setStatusBarMessage(
					"Please save before running strategy ...\n",
					BasePanel.WARNING);
		} else {
			runStrategy(m_tradingdays, true);
		}
	}

	/**
	 * This is fired from the Contract/Tradingday Tab when the Broker data
	 * button is pressed. It is also fired doExceutionDetailEnd(). This should
	 * be used to fetch executions for orders that may have been filled while
	 * the system was down.
	 * 
	 * @param tradestrategy
	 *            the Tradestrategy for which you are requesting historical
	 *            data.
	 * 
	 */

	public void doData(final Tradestrategy tradestrategy) {
		if (tradestrategy.isDirty()) {
			this.setStatusBarMessage(
					"Please save or refresh before running strategy ...\n",
					BasePanel.WARNING);
		} else {
			Tradingdays tradingdays = new Tradingdays();
			Tradingday tradingday = Tradingday.newInstance(tradestrategy
					.getTradingday().getOpen());
			tradingday.addTradestrategy(tradestrategy);
			tradingdays.add(tradingday);
			runStrategy(tradingdays, true);
		}
	}

	/**
	 * This is fired from any Tab when the Delete button is pressed. This should
	 * be used to delete all the trading orders for the current trading days.
	 * 
	 */

	public void doDelete() {
		try {
			deleteTradeOrders(m_tradingdays);

		} catch (Exception ex) {
			this.setErrorMessage("Error deleting TradeOrders.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This is fired from the Trading Tab when the Delete button is pressed.
	 * This should be used to delete a trade orders for a selected
	 * tradestrategy.
	 * 
	 * @param tradestrategy
	 *            the Tradestrategy that you would like to delete tradeorders
	 *            for.
	 * 
	 */

	public void doDelete(final Tradestrategy tradestrategy) {
		try {
			Tradingdays tradingdays = new Tradingdays();
			Tradingday tradingday = Tradingday.newInstance(tradestrategy
					.getTradingday().getOpen());
			tradingday.addTradestrategy(tradestrategy);
			tradingdays.add(tradingday);
			deleteTradeOrders(tradingdays);

		} catch (Exception ex) {
			this.setErrorMessage("Error deleting TradeOrders.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This is fired from the Contract Tab when the Execute Order button is
	 * pressed. This should be used to execute orders to the broker platform.
	 * 
	
	 * 
	 * @param instance TradeOrder
	 */

	public void doExecute(TradeOrder instance) {

		try {
			this.getFrame().setCursor(
					Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			TradeOrder tradeOrder = m_tradePersistentModel
					.findTradeOrderByKey(instance.getOrderKey());
			if (null != tradeOrder) {
				if (!tradeOrder.getVersion().equals(instance.getVersion())) {
					this.setStatusBarMessage(
							"Please refresh order before sumbitting change ...\n",
							BasePanel.WARNING);
				}
			}
			Tradestrategy tradestrategy = m_tradePersistentModel
					.findTradestrategyById(instance.getTrade()
							.getTradestrategy());
			instance = m_brokerModel.onPlaceOrder(tradestrategy.getContract(),
					instance);
			setStatusBarMessage("Order sent to broker.\n",
					BasePanel.INFORMATION);

		} catch (Exception ex) {
			this.setErrorMessage(
					"Error submitting Order " + instance.getOrderKey(),
					ex.getMessage(), ex);
		} finally {
			this.getFrame().setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * This is fired from the main menu when the Run Strategy button is pressed.
	 * This will run the Strategy for all the tradingdays.
	 * 
	 * 
	 */

	public void doRun() {
		try {
			if (m_tradingdays.isDirty()) {
				this.setStatusBarMessage(
						"Please save or refresh before running strategy ...\n",
						BasePanel.WARNING);
			} else {
				Tradingday today = m_tradingdays.getTradingdays().get(
						TradingCalendar.getBusinessDayStart(new Date()));
				if (null == today) {
					this.setStatusBarMessage(
							"Market is not open for any of the selected trading days ...\n",
							BasePanel.INFORMATION);
				} else {
					runStrategy(m_tradingdays, false);
				}
			}
		} catch (Exception ex) {
			this.setErrorMessage("Error running Trade Strategies.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This is fired from the Tradingday Tab when the Run Strategy button is
	 * pressed. This will run the Strategy for all the tradingdays.
	 * 
	 * 
	 * @param tradestrategy Tradestrategy
	 */

	public void doRun(final Tradestrategy tradestrategy) {
		try {
			if (tradestrategy.isDirty()) {
				this.setStatusBarMessage(
						"Please save or refresh before running strategy ...\n",
						BasePanel.WARNING);
			} else {
				Tradingdays tradingdays = new Tradingdays();
				Tradingday tradingday = Tradingday.newInstance(tradestrategy
						.getTradingday().getOpen());
				tradingday.addTradestrategy(tradestrategy);
				tradingdays.add(tradingday);
				runStrategy(tradingdays, false);
			}
		} catch (Exception ex) {
			this.setErrorMessage("Error running Trade Strategies.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This is fired from the main menu when the Back Test Strategy button is
	 * pressed. This will run the Strategy for all the tradingdays.
	 * 
	 * 
	 */

	public void doTest() {
		if (m_tradingdays.isDirty()) {
			this.setStatusBarMessage(
					"Please save before running strategy ...\n",
					BasePanel.WARNING);
		} else {
			contractPanel.doCloseAll();
			runStrategy(m_tradingdays, false);
		}
	}

	/**
	 * This is fired from the Tradingday Tab when the Back Test Strategy button
	 * is pressed. This will run the Strategy for the selected tradingday.
	 * 
	
	 * 
	 * @param tradestrategy Tradestrategy
	 */

	public void doTest(Tradestrategy tradestrategy) {

		if (tradestrategy.isDirty()) {
			this.setStatusBarMessage(
					"Please save before running strategy ...\n",
					BasePanel.WARNING);
		} else {
			contractPanel.doClose(tradestrategy);
			Tradingdays tradingdays = new Tradingdays();
			Tradingday tradingday = Tradingday.newInstance(tradestrategy
					.getTradingday().getOpen());
			tradingday.addTradestrategy(tradestrategy);
			tradingdays.add(tradingday);
			runStrategy(tradingdays, false);
		}
	}

	/**
	 * This is fired from the Tradingday Tab when the Save button on the toolbar
	 * is pressed. This will save all the tradingdays/tradestrategies.
	 * 
	 */

	public void doSave() {
		try {

			this.setStatusBarMessage("Save in progress ...\n",
					BasePanel.INFORMATION);

			// Save the Trading days
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						getFrame().setCursor(
								Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						boolean dirty = false;
						for (Tradingday tradingday : m_tradingdays
								.getTradingdays().values()) {
							if (tradingday.isDirty()) {
								dirty = true;
								for (Tradestrategy tradestrategy : tradingday
										.getTradestrategies()) {
									if (tradestrategy.isDirty()) {
										if (null != tradestrategy
												.getIdTradeStrategy()) {
											m_brokerModel
													.onCancelRealtimeBars(tradestrategy);
											tradestrategy
													.setDatasetContainer(null);
										}
										contractPanel.doClose(tradestrategy);
									}
								}
								m_tradePersistentModel
										.persistTradingday(tradingday);
							}
						}
						if (dirty)
							refreshTradingdays(m_tradingdays);
						clearStatusBarMessage();
						getFrame().setCursor(Cursor.getDefaultCursor());
					} catch (PersistentModelException ex) {
						setErrorMessage("Error saving Trade Strategies.",
								ex.getMessage(), ex);
					}
				}
			});

		} catch (Exception ex) {
			this.setErrorMessage("Error saving Trade Strategies.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method is fired when the system connects to TWS, if there are open
	 * orders. i.e from a BrokerModel event. If todays orders are not in the
	 * openTradeOrders then we cancel then order.
	 * 
	 * @param openTradeOrders
	 *            Hashtable<Integer, TradeOrder> the open orders that are from
	 *            IB TWS.
	
	 * @see org.trade.broker.BrokerChangeListener#openOrderEnd(ConcurrentHashMap<Integer,TradeOrder>)
	 */

	public void openOrderEnd(
			ConcurrentHashMap<Integer, TradeOrder> openTradeOrders) {
		try {

			_log.info("Open orders received from TWS: "
					+ openTradeOrders.size());
			Tradingday todayTradingday = m_tradingdays
					.getTradingday(TradingCalendar.getTodayBusinessDayStart());
			if (null == todayTradingday) {
				return;
			}
			/*
			 * Cancel any orders that were open and not filled.
			 */
			for (Tradestrategy tradestrategy : todayTradingday
					.getTradestrategies()) {
				if (null != tradestrategy.getOpenTrade()) {
					Trade trade = m_tradePersistentModel
							.findTradeById(tradestrategy.getOpenTrade()
									.getIdTrade());
					for (TradeOrder todayTradeOrder : trade.getTradeOrders()) {
						if (!todayTradeOrder.getIsFilled()) {
							if (!openTradeOrders.containsKey(todayTradeOrder
									.getOrderKey())) {
								todayTradeOrder
										.setStatus(OrderStatus.CANCELLED);
								m_tradePersistentModel
										.persistTradeOrder(todayTradeOrder);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			this.setErrorMessage("Error starting PositionManagerRule.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method is fired when the Brokermodel has completed the request for
	 * Execution Details see doFetchExecution or connectionOpened i.e from a
	 * BrokerModel event all executions for the filter have now been received.
	 * Check to see if we need to close any trades for these order fills.
	 * 
	 * @param tradeOrders
	 *            Hashtable<Integer, TradeOrder> the executed and open orders
	 *            that are from IB TWS.
	 * 
	
	 * @see org.trade.broker.BrokerChangeListener#executionDetailsEnd(ConcurrentHashMap<Integer,TradeOrder>)
	 */
	public void executionDetailsEnd(
			ConcurrentHashMap<Integer, TradeOrder> tradeOrders) {
		try {
			Tradingday todayTradingday = m_tradingdays
					.getTradingday(TradingCalendar.getTodayBusinessDayStart());
			if (null == todayTradingday) {
				return;
			}
			m_brokerModel.onReqOpenOrders();

		} catch (Exception ex) {
			this.setErrorMessage("Error starting PositionManagerRule.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method is fired when the Brokermodel has completed
	 * executionDetails() or openOrder() and the order that was FILLED. If the
	 * order opens a position and the stop price is set then this is an open
	 * order created via a strategy. Check to see that we have a strategy
	 * manager if so start the manager and close the strategy that opened the
	 * position.
	 * 
	
	 * 
	
	 * @param tradeOrder TradeOrder
	 * @see org.trade.broker.BrokerChangeListener#tradeOrderFilled(TradeOrder)
	 */
	public void tradeOrderFilled(TradeOrder tradeOrder) {

		try {

			/*
			 * If the order opens a position and the stop price is set then this
			 * is an open order created via a strategy. Check to see that we
			 * have a strategy manager if so start the manager and close the
			 * strategy that opened the position.
			 */
			if (tradeOrder.getIsOpenPosition()
					&& null != tradeOrder.getStopPrice()) {

				Tradestrategy tradestrategy = m_tradingdays
						.getTradestrategy(tradeOrder.getTrade()
								.getTradestrategyId().getIdTradeStrategy());

				if (null == tradestrategy) {
					this.setStatusBarMessage(
							"Warning position opened but Tradestrategy not found for Order Key: "
									+ tradeOrder.getOrderKey()
									+ " in the current Tradingday Tab selection.",
							BasePanel.WARNING);
					return;
				}
				if (!tradestrategy.getTrade()) {
					this.setStatusBarMessage(
							"Warning position opened for Symbol: "
									+ tradestrategy.getContract().getSymbol()
									+ "  but this tradestrategy is not set to trade. A manual order was created Key: "
									+ tradeOrder.getOrderKey(),
							BasePanel.WARNING);
					return;
				}
				/*
				 * If this Strategy has a manager start the Strategy Manager.
				 */

				if (null != tradestrategy.getStrategy().getStrategyManager()) {

					if (!this.isStrategyWorkerRunning(tradestrategy
							.getStrategy().getStrategyManager().getClassName()
							+ tradestrategy.getIdTradeStrategy())) {
						/*
						 * Kill the worker that got us in if still running its
						 * job is done.
						 */

						killStrategyWorker(tradestrategy.getStrategy()
								.getClassName()
								+ tradestrategy.getIdTradeStrategy());

						_log.info("Start PositionManagerStrategy: "
								+ tradestrategy.getContract().getSymbol());
						_log.info("tradeOrderFilled Trade Id: "
								+ tradeOrder.getTrade().getIdTrade()
								+ " Version: "
								+ tradeOrder.getTrade().getVersion());
						createStrategy(tradestrategy.getStrategy()
								.getStrategyManager().getClassName(),
								tradestrategy);
					}
				}
			}
		} catch (Exception ex) {
			this.setErrorMessage("Error starting PositionManagerRule.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method is fired when the Brokermodel has completed
	 * executionDetails() or openOrder() and the order that was CANCELLED.
	 * 
	
	 * 
	
	 * @param tradeOrder TradeOrder
	 * @see org.trade.broker.BrokerChangeListener#tradeOrderCancelled(TradeOrder)
	 */
	public void tradeOrderCancelled(TradeOrder tradeOrder) {

		try {
			Tradestrategy tradestrategy = m_tradingdays
					.getTradestrategy(tradeOrder.getTrade()
							.getTradestrategyId().getIdTradeStrategy());
			_log.info("Trade Order cancelled for Symbol: "
					+ tradestrategy.getContract().getSymbol() + " order key: "
					+ tradeOrder.getOrderKey());
		} catch (Exception ex) {
			this.setErrorMessage("Error starting PositionManagerRule.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method is fired when the Brokermodel has completed
	 * executionDetails() or openOrder() and the position was closed by the
	 * order.
	 * 
	
	 * 
	
	 * @param trade Trade
	 * @see org.trade.broker.BrokerChangeListener#positionClosed(Trade)
	 */
	public void positionClosed(Trade trade) {
		try {
			Tradestrategy tradestrategy = m_tradingdays.getTradestrategy(trade
					.getTradestrategyId().getIdTradeStrategy());
			_log.info("Trade closed for Symbol: "
					+ tradestrategy.getContract().getSymbol()
					+ " Profit/Loss: " + trade.getProfitLoss());

		} catch (Exception ex) {
			this.setErrorMessage("Error position closed : ", ex.getMessage(),
					ex);
		}
	}

	/**
	 * Method strategyComplete.
	 * @param tradestrategy Tradestrategy
	 * @see org.trade.strategy.StrategyChangeListener#strategyComplete(Tradestrategy)
	 */
	public void strategyComplete(Tradestrategy tradestrategy) {

		try {
			if (m_brokerModel.isConnected()) {
				Tradestrategy reFreshedTradestrategy = m_tradePersistentModel
						.findTradestrategyById(tradestrategy
								.getIdTradeStrategy());
				m_tradingdays.getTradestrategy(
						tradestrategy.getIdTradeStrategy()).setStatus(
						reFreshedTradestrategy.getStatus());
				contractPanel.doRefresh(tradestrategy);
			}

		} catch (Exception ex) {
			this.setErrorMessage("Error strategyComplete : ", ex.getMessage(),
					ex);
		}
	}

	/**
	 * Method strategyStarted.
	 * @param tradestrategy Tradestrategy
	 * @see org.trade.strategy.StrategyChangeListener#strategyStarted(Tradestrategy)
	 */
	public void strategyStarted(Tradestrategy tradestrategy) {

	}

	/**
	 * Method ruleComplete.
	 * @param tradestrategy Tradestrategy
	 * @see org.trade.strategy.StrategyChangeListener#ruleComplete(Tradestrategy)
	 */
	public void ruleComplete(Tradestrategy tradestrategy) {

	}

	/**
	 * Method positionCovered.
	 * @param tradestrategy Tradestrategy
	 * @see org.trade.strategy.StrategyChangeListener#positionCovered(Tradestrategy)
	 */
	public void positionCovered(Tradestrategy tradestrategy) {
		try {
			if (m_brokerModel.isConnected())
				contractPanel.doRefresh(tradestrategy);
		} catch (Exception ex) {
			this.setErrorMessage("Error positionCovered : ", ex.getMessage(),
					ex);
		}
	}

	/**
	 * Method strategyError.
	 * @param ex StrategyRuleException
	 * @see org.trade.strategy.StrategyChangeListener#strategyError(StrategyRuleException)
	 */
	public void strategyError(StrategyRuleException ex) {
		if (ex.getErrorId() == 1) {
			this.setErrorMessage("Error: " + ex.getErrorCode(),
					ex.getMessage(), ex);
		} else if (ex.getErrorId() == 2) {
			this.setStatusBarMessage("Warning: " + ex.getMessage(),
					BasePanel.WARNING);
		} else if (ex.getErrorId() == 3) {
			this.setStatusBarMessage("Information: " + ex.getMessage(),
					BasePanel.INFORMATION);
		} else {

			this.setErrorMessage("Unknown Error Id Code: " + ex.getErrorCode(),
					ex.getMessage(), ex);
		}
		this.getFrame().setCursor(Cursor.getDefaultCursor());
	}

	public void doHelp() {
		doAbout();
	}

	/**
	 * This method is fired from the main menu. It displays the application
	 * version.
	 * 
	 */
	public void doAbout() {
		try {
			StringBuffer message = new StringBuffer();
			message.append("Product version: ");
			message.append(TradeMainControllerPanel.version);
			message.append("\nBuild Label:     ");
			message.append(TradeMainControllerPanel.title);
			message.append("\nBuild Time:      ");
			message.append(TradeMainControllerPanel.date);
			JOptionPane.showMessageDialog(this, message, "About Help",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex) {
			this.setErrorMessage("Could not load about help.", ex.getMessage(),
					ex);
		}
	}

	/**
	 * This method is fired from the Broker API on completion of broker data
	 * request. Note if this is the current trading day for this trade strategy
	 * real time data has been started by the broker interface. Check to see if
	 * a trade is already open for this trade strategy. If so fire up a trade
	 * manager. If not fire of the strategy.
	 * 
	 * @param tradestrategy
	 *            Tradestrategy that has completed the request for historical
	 *            data
	 * 
	
	 * @see org.trade.broker.BrokerChangeListener#historicalDataComplete(Tradestrategy)
	 */

	public void historicalDataComplete(Tradestrategy tradestrategy) {
		try {
			/*
			 * Now we have the history data complete and the request for real
			 * time data has started, so fire of the strategy for this
			 * tradestrategy.
			 */
			if (m_brokerModel.isBrokerDataOnly()) {
				if (!m_brokerModel.isConnected()) {
					m_brokerModel.onCancelBrokerData(tradestrategy);
				}
			} else {
				if (tradestrategy.getTrade()) {
					boolean isOpen = false;
					for (Trade trade : tradestrategy.getTrades()) {
						if (trade.getIsOpen()
								&& trade.getOpenPosition().getIsFilled()) {
							isOpen = true;
							int result = JOptionPane.showConfirmDialog(this
									.getFrame(), "Position is open for: "
									+ tradestrategy.getContract().getSymbol()
									+ " do you want to run Position Mgr ?",
									"Information", JOptionPane.YES_NO_OPTION);
							if (result == JOptionPane.YES_OPTION) {
								createStrategy(tradestrategy.getStrategy()
										.getStrategyManager().getClassName(),
										tradestrategy);
								break;
							} else {
								int result1 = JOptionPane
										.showConfirmDialog(
												this.getFrame(),
												"Position is open for: "
														+ tradestrategy
																.getContract()
																.getSymbol()
														+ " do you want to delete all Orders?",
												"Information",
												JOptionPane.YES_NO_OPTION);
								if (result1 == JOptionPane.YES_OPTION) {
									m_tradePersistentModel
											.removeTradestrategyTrades(tradestrategy);
									break;
								}
							}
						}
					}
					if (!isOpen) {
						createStrategy(tradestrategy.getStrategy()
								.getClassName(), tradestrategy);
					}
				}
			}

		} catch (Exception ex) {
			this.setErrorMessage("Could not start strategy: "
					+ tradestrategy.getStrategy().getName() + " for Symbol: "
					+ tradestrategy.getContract().getSymbol(), ex.getMessage(),
					ex);
		}
	}

	/**
	 * This method connects to the Broker Platform and is fired when the main
	 * menu item connect is pressed..
	 * 
	 */

	public void doConnect() {
		try {

			if ((null != m_brokerModel) && m_brokerModel.isConnected()) {
				int result = JOptionPane.showConfirmDialog(this.getFrame(),
						"Already connected. Do you want to disconnect?",
						"Information", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					doDisconnect();
				}
			}
			ConnectionPane connectionPane = new ConnectionPane();
			TextDialog dialog = new TextDialog(this.getFrame(),
					"Connect to TWS", true, connectionPane);
			dialog.getCancelButton().setText("Test");
			dialog.getOKButton().setText("Live");
			dialog.setLocationRelativeTo(this);
			dialog.setVisible(true);

			if (!dialog.getCancel()) {
				m_brokerModel = (BrokerModel) ClassFactory
						.getServiceForInterface(BrokerModel._broker, this);
				this.getFrame().setCursor(
						Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				this.setStatusBarMessage("Please wait while login proceeds",
						BasePanel.INFORMATION);
				/*
				 * Controller listens for problems from the TWS interface see
				 * doError()
				 */
				m_brokerModel.addMessageListener(this);
				m_brokerModel.onConnect(connectionPane.getHost(),
						connectionPane.getPort(), connectionPane.getClientId());
				simulatedMode(false);
				this.setStatusBarMessage("Running live.", BasePanel.INFORMATION);
			} else {
				this.setStatusBarMessage("Running in test.",
						BasePanel.INFORMATION);
			}
		} catch (Exception ex) {
			this.setErrorMessage("Could Not Connect/Disconnect From TWS",
					ex.getMessage(), ex);

		} finally {
			this.getFrame().setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * This method is fired after the tab has been created and placed in the tab
	 * controller.
	 * 
	 */

	public void doWindowOpen() {
		doConnect();
	}

	/**
	 * This method is fired when the tab closes.
	 * 
	 */

	public void doWindowClose() {
		killAllStrategyWorker();
		doDisconnect();
		doExit();
	}

	/**
	 * This method is fired from an event in the Broker Model. All exception
	 * reported back from the broker interface are received here.
	 * 
	 * 0 - 999 are IB TWS error codes for Orders or data 1000 - 1999 are IB TWS
	 * System error 2000 - 2999 are IB TWS Warning 4000 - 4999 are application
	 * warnings 5000 - 5999 are application information
	 * 
	 * @param ex
	 *            BrokerManagerModelException the broker exception
	 * @see org.trade.broker.BrokerChangeListener#brokerError(BrokerModelException)
	 */

	public void brokerError(BrokerModelException ex) {

		if (ex.getErrorId() == 1) {
			this.setErrorMessage("Error: " + ex.getErrorCode(),
					ex.getMessage(), ex);
		} else if (ex.getErrorId() == 2) {
			this.setStatusBarMessage("Warning: " + ex.getMessage(),
					BasePanel.WARNING);
		} else if (ex.getErrorId() == 3) {
			this.setStatusBarMessage("Information: " + ex.getMessage(),
					BasePanel.INFORMATION);
		} else {

			this.setErrorMessage("Unknown Error Id Code: " + ex.getErrorCode(),
					ex.getMessage(), ex);
		}
		this.getFrame().setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * This method is disconnects from the Broker Platform and is fired when the
	 * main menu item disconnect is pressed..
	 * 
	 */

	public void doDisconnect() {
		try {
			killAllStrategyWorker();
			if (m_brokerModel.isConnected()) {
				doCancel();
				m_brokerModel.disconnect();
			}
		} catch (BrokerModelException ex) {
			this.setErrorMessage("Could Not Disconnect From TWS",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method is fired from an event in the Broker Model. A connection has
	 * been opened.
	 * 
	 * @see org.trade.broker.BrokerChangeListener#connectionOpened()
	 */

	public void connectionOpened() {

		try {

			tradingdayPanel.setConnected(true);
			contractPanel.setConnected(true);
			simulatedMode(false);
			Tradingday todayTradingday = m_tradingdays
					.getTradingday(TradingCalendar.getTodayBusinessDayStart());

			/*
			 * Request all the executions for today. This will result in updates
			 * to any trade orders that were filled while we were disconnected.
			 */
			if (null != todayTradingday) {
				m_brokerModel.onReqAllExecutions(todayTradingday.getOpen());
			}

		} catch (Exception ex) {
			this.setErrorMessage("Error finding excecutions.", ex.getMessage(),
					ex);
		}
	}

	/**
	 * This method is fired from an event in the Broker Model. A connection has
	 * been closed.
	 * 
	 * @see org.trade.broker.BrokerChangeListener#connectionClosed()
	 */
	public void connectionClosed() {
		tradingdayPanel.setConnected(false);
		contractPanel.setConnected(false);
		simulatedMode(true);
		this.setStatusBarMessage("Connected to Broker was closed.",
				BasePanel.WARNING);
	}

	/**
	 * This method is fired from an event in the Broker Model. The managed
	 * account for this connection. Note each instance of TWS is connected to
	 * one account only. As login in TWS are by account.
	 * 
	 * @param accountNumber
	 *            String the account number.
	 * @see org.trade.broker.BrokerChangeListener#managedAccountsUpdated(String)
	 */

	public void managedAccountsUpdated(String accountNumber) {
		try {

			TradeAccount tradeAccount = m_tradePersistentModel
					.findTradeAccountByNumber(accountNumber);
			if (null == tradeAccount) {
				tradeAccount = new TradeAccount(accountNumber, accountNumber,
						Currency.USD, true);
				tradeAccount = (TradeAccount) m_tradePersistentModel
						.persistAspect(tradeAccount);
				m_tradePersistentModel.resetDefaultTradeAccount(tradeAccount);
				DBTableLookupServiceProvider.clearLookup();
				tradingdayPanel.doWindowActivated();
			} else {
				if (!tradeAccount.getIsDefault()) {
					tradeAccount.setIsDefault(true);
					m_tradePersistentModel
							.resetDefaultTradeAccount(tradeAccount);
				}
			}
			m_brokerModel.onSubscribeAccountUpdates(true, tradeAccount);
			this.setStatusBarMessage("Connected to IB Account: "
					+ accountNumber, BasePanel.INFORMATION);
		} catch (Exception ex) {
			this.setErrorMessage("Could not retreive account data Msg: ",
					ex.getMessage(), ex);
		}
	}

	/**
	 * Method updateAccountTime.
	 * @param accountNumber String
	 * @see org.trade.broker.BrokerChangeListener#updateAccountTime(String)
	 */
	public void updateAccountTime(String accountNumber) {
		try {
			TradeAccount tradeAccount = m_tradePersistentModel
					.findTradeAccountByNumber(accountNumber);
			tradingdayPanel.setTradeAccountLabel(tradeAccount);
			this.setStatusBarMessage("Connected to IB Account: "
					+ accountNumber, BasePanel.INFORMATION);
		} catch (Exception ex) {
			this.setErrorMessage("Could not retreive account data Msg: ",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method retrieves all the details about a contract.
	 * 
	 */

	public void doProperties() {
		try {
			for (Tradingday tradingday : m_tradingdays.getTradingdays()
					.values()) {
				for (Tradestrategy tradestrategy : tradingday
						.getTradestrategies()) {
					m_brokerModel
							.onContractDetails(tradestrategy.getContract());
				}
			}
		} catch (BrokerModelException ex) {
			this.setErrorMessage("Could not disconnect From TWS",
					ex.getMessage(), ex);
		}
	}

	/**
	 * This method is fired from the Contract Tab when the Cancel Order button
	 * is pressed. This should be used to cancel orders in the broker platform.
	 * 
	 * @param order
	 *            the TradeOrder that you would like to cancel.
	 * 
	 */

	public void doCancel(TradeOrder order) {

		if (!order.getIsFilled()) {
			try {
				m_brokerModel.onCancelOrder(order);
			} catch (BrokerModelException ex) {
				this.setErrorMessage(
						"Error cancelling Order " + order.getOrderKey(),
						ex.getMessage(), ex);
			}

		} else {
			this.setStatusBarMessage("Order is filled and cannot be cancelled",
					BasePanel.INFORMATION);
		}
	}

	/**
	 * This method is fired from the Cancel Live data button on the main tool
	 * bar. This will cancel all live data and all strategies that are running.
	 * 
	 * 
	 */

	public void doCancel() {

		// Cancel the candleWorker if running
		m_brokerModel.onCancelAllRealtimeData();
		if ((null != brokerDataRequestProgressMonitor)
				&& !brokerDataRequestProgressMonitor.isDone()) {
			brokerDataRequestProgressMonitor.cancel(true);
		}
		killAllStrategyWorker();
		m_indicatorTradestrategy.clear();
		refreshTradingdays(m_tradingdays);
		this.setStatusBarMessage(
				"Strategies and live data have been cancelled.",
				BasePanel.INFORMATION);
	}

	/**
	 * This method is fired from the Contract Tab or Trading Tab when the Cancel
	 * Strategy button is pressed. This should be used to cancel strategies in
	 * the broker platform.
	 * 
	 * @param tradestrategy
	 *            the Tradestrategy that you would like to cancel.
	 * 
	 */

	public void doCancel(Tradestrategy tradestrategy) {
		try {
			if (m_brokerModel.isRealtimeBarsRunning(tradestrategy)) {
				m_brokerModel.onCancelRealtimeBars(tradestrategy);
				this.setStatusBarMessage(
						"Realtime data has been cancelled for Symbol: "
								+ tradestrategy.getContract().getSymbol(),
						BasePanel.INFORMATION);
			}
			// Cancel the StrategyWorker if running
			if (this.isStrategyWorkerRunning(tradestrategy)) {
				killAllStrategyWorkersForTradestrategy(tradestrategy);
				this.setStatusBarMessage(
						"Strategy has been cancelled for Symbol: "
								+ tradestrategy.getContract().getSymbol(),
						BasePanel.INFORMATION);
			}
		} catch (Exception ex) {
			this.setErrorMessage("Could not cancel strategy.", ex.getMessage(),
					ex);
		}
	}

	/**
	 * This method is fired from the Main menu and will allow you to setup the
	 * printer setting.
	 */

	public void doPrintSetup() {
		doPrint();
	}

	/**
	 * This method is fired from the Main menu and will allow you to preview a
	 * print of the current tab.
	 */
	public void doPrintPreview() {
		doPrint();
	}

	/**
	 * This method is fired from the Main menu and will allow you to print the
	 * current tab.
	 */
	public void doPrint() {
		try {

			PrinterJob pj = PrinterJob.getPrinterJob();
			PageFormat pageFormat = new PageFormat();
			ComponentPrintService vista = new ComponentPrintService(
					((JFrame) this.getFrame()).getContentPane(), pageFormat);
			vista.scaleToFit(true);

			pj.validatePage(pageFormat);
			pj.setPageable(vista);

			if (pj.printDialog()) {
				pj.print();
			}

		} catch (Exception ex) {
			_log.error("Error printing msg: " + ex.getMessage(), ex);
		}
	}

	/**
	 * This is fired from the main menu when the assigning Strategy button is
	 * pressed. This will re assign all the tradestrategies..
	 * 
	 * 
	 * @param strategies List<Strategy>
	 */

	public void doReAssign(List<Strategy> strategies) {

		try {
			/*
			 * Check to see if any of the selected trading days has open
			 * positions. If they do kill the strategy worker before deleting
			 * trades.
			 */
			for (Tradingday tradingday : m_tradingdays.getTradingdays()
					.values()) {
				if (Tradingdays.hasTrades(tradingday)) {
					JOptionPane
							.showMessageDialog(
									this.getFrame(),
									"Tradingday: "
											+ tradingday.getOpen()
											+ " has trades. Please delete all trades before re-asigning strategies.",
									"Warning", JOptionPane.OK_OPTION);
					return;
				}
			}

			int result = JOptionPane
					.showConfirmDialog(
							this.getFrame(),
							"Are you sure you want to re-assign strategies for selected trading days?",
							"Warning", JOptionPane.YES_NO_OPTION);

			if (result == JOptionPane.YES_OPTION) {
				this.setStatusBarMessage("Reassign in progress ...\n",
						BasePanel.INFORMATION);
				final ReAssignProgressMonitor reAssignProgressMonitor = new ReAssignProgressMonitor(
						m_tradePersistentModel, m_tradingdays,
						strategies.get(0), strategies.get(1));
				reAssignProgressMonitor
						.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
							public void propertyChange(PropertyChangeEvent evt) {
								if ("progress".equals(evt.getPropertyName())) {
									int progress = (Integer) evt.getNewValue();
									setProgressBarProgress(progress,
											reAssignProgressMonitor);
								}
							}
						});
				reAssignProgressMonitor.execute();
			}

		} catch (Exception ex) {
			this.setErrorMessage("Error re-assigning Strategies.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * Method doTransfer.
	 * @param idTradestrategy Integer
	 */
	public void doTransfer(Integer idTradestrategy) {
		try {
			Tradestrategy tradestrategy = m_tradingdays
					.getTradestrategy(idTradestrategy);
			if (null == tradestrategy) {
				tradestrategy = m_tradePersistentModel
						.findTradestrategyById(idTradestrategy);
			}
			if (null == m_tradingdays.getTradingday(tradestrategy
					.getTradingday().getOpen())) {
				Tradingday tradingday = m_tradePersistentModel
						.findTradingdayById(tradestrategy.getTradingday()
								.getIdTradingDay());
				m_tradingdays.add(tradingday);
			}
			if (tradestrategy.isDirty()) {
				setStatusBarMessage("Please save ...\n", BasePanel.WARNING);
			} else {
				contractPanel.doTransfer(tradestrategy);
				this.setSelectPanel(contractPanel);
			}
		} catch (PersistentModelException ex) {
			this.setErrorMessage("Error finding Tradingday.", ex.getMessage(),
					ex);
		}
	}

	/**
	 * Method tabChanged.
	 * @param currBasePanel BasePanel
	 * @param newBasePanel BasePanel
	 */
	public void tabChanged(BasePanel currBasePanel, BasePanel newBasePanel) {
		this.m_menuBar.setEnabledDeleteSave(false);
		this.m_menuBar.setEnabledRunStrategy(false);
		this.m_menuBar.setEnabledBrokerData(false);
		this.m_menuBar.setEnabledTestStrategy(false);
		if (tradingdayPanel == newBasePanel) {
			if (null == brokerDataRequestProgressMonitor
					|| brokerDataRequestProgressMonitor.isDone()) {
				this.m_menuBar.setEnabledDeleteSave(true);
				if (m_brokerModel.isConnected()) {
					this.m_menuBar.setEnabledRunStrategy(true);
				} else {
					this.m_menuBar.setEnabledTestStrategy(true);
				}
				this.m_menuBar.setEnabledBrokerData(true);
			}
		}
	}

	/**
	 * Method deleteTradeOrders.
	 * @param tradingdays Tradingdays
	 */
	private void deleteTradeOrders(Tradingdays tradingdays) {

		/*
		 * Check to see if any of the selected trading days has open positions.
		 * If they do kill the strategy worker before deleting trades.
		 */
		for (Tradingday tradingday : tradingdays.getTradingdays().values()) {
			if (Tradingdays.hasOpenTrades(tradingday)) {
				int result = JOptionPane
						.showConfirmDialog(
								this.getFrame(),
								"Tradingday: "
										+ tradingday.getOpen()
										+ " has open positions. Do you want to continue",
								"Warning", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION) {
					removeStrategyWorker(tradingday);
				} else {
					return;
				}
			}
		}

		int result = JOptionPane.showConfirmDialog(this.getFrame(),
				"Are you sure you want to delete all Trade Orders?", "Warning",
				JOptionPane.YES_NO_OPTION);
		this.killAllStrategyWorker();
		if (result == JOptionPane.YES_OPTION) {
			this.setStatusBarMessage("Delete in progress ...\n",
					BasePanel.INFORMATION);
			final DeleteProgressMonitor deleteProgressMonitor = new DeleteProgressMonitor(
					m_tradePersistentModel, tradingdays);
			deleteProgressMonitor
					.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent evt) {
							if ("progress".equals(evt.getPropertyName())) {
								int progress = (Integer) evt.getNewValue();
								setProgressBarProgress(progress,
										deleteProgressMonitor);
							}
						}
					});
			deleteProgressMonitor.execute();
		}
	}

	/**
	 * Method runStrategy.
	 * @param tradingdays Tradingdays
	 * @param brokerDataOnly boolean
	 */
	private void runStrategy(Tradingdays tradingdays, boolean brokerDataOnly) {
		try {
			m_brokerModel.setBrokerDataOnly(brokerDataOnly);
			if ((null != brokerDataRequestProgressMonitor)
					&& !brokerDataRequestProgressMonitor.isDone()) {
				this.setStatusBarMessage(
						"Strategies already running please wait or cancel ...",
						BasePanel.INFORMATION);
				return;
			} else {
				if (brokerDataOnly && !m_brokerModel.isConnected()) {
					int result = JOptionPane
							.showConfirmDialog(
									this.getFrame(),
									"Yahoo Finance will be used to retrieve candle data."
											+ "\n"
											+ "Do you want to continue ?"
											+ "\n"
											+ "Note there is a 20min delay to data. This option should "
											+ " \n"
											+ "only be used 30mins after market close."
											+ "\n"
											+ "Valid Bar Size/Chart Hist vales are:"
											+ "\n"
											+ "Chart Hist = 1 D, Bar Size >= 1min"
											+ "\n"
											+ "Chart Hist > 1 D to 1 W, Bar Size >= 5min"
											+ "\n"
											+ "Chart Hist > 1 D to 3 M, Bar Size = 1 day",
									"Information", JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.NO_OPTION) {
						return;
					}
				}
				for (Tradingday tradingday : tradingdays.getTradingdays()
						.values()) {
					if (isStrategyWorkerRunning(tradingday)) {
						this.setStatusBarMessage(
								"Strategies already running please wait or cancel ...",
								BasePanel.INFORMATION);
						return;
					}
					if (Tradingdays.hasTrades(tradingday) && !brokerDataOnly) {
						int result = JOptionPane
								.showConfirmDialog(
										this.getFrame(),
										"Trading strategy cannot be run as Trading day:"
												+ tradingday.getOpen()
												+ " has trades. Do you want to delete trades?",
										"Information",
										JOptionPane.YES_NO_OPTION);
						if (result == JOptionPane.YES_OPTION) {
							m_tradePersistentModel
									.removeTradingdayTrades(tradingday);
						} else {
							if (!m_brokerModel.isConnected()) {
								return;
							}
						}
					}
					for (Tradestrategy tradestrategy : tradingday
							.getTradestrategies()) {
						if (m_brokerModel.isRealtimeBarsRunning(tradestrategy)
								|| m_brokerModel
										.isMarketDataRunning(tradestrategy)) {
							int result = JOptionPane.showConfirmDialog(this
									.getFrame(),
									"A real time data request is already running for Symbol: "
											+ tradestrategy.getContract()
													.getSymbol()
											+ " Do you want to cancel?",
									"Information", JOptionPane.YES_NO_OPTION);
							if (result == JOptionPane.YES_OPTION) {
								m_brokerModel
										.onCancelRealtimeBars(tradestrategy);
							}
						}
						if (brokerDataOnly && !m_brokerModel.isConnected()) {
							Date endDate = TradingCalendar
									.getBusinessDayEnd(TradingCalendar
											.getMostRecentTradingDay(TradingCalendar
													.addBusinessDays(
															tradestrategy
																	.getTradingday()
																	.getClose(),
															0)));
							Date startDate = TradingCalendar.addDays(endDate,
									(-1 * (tradestrategy.getChartDays() - 1)));
							startDate = TradingCalendar
									.getMostRecentTradingDay(startDate);

							List<Candle> candles = m_tradePersistentModel
									.findCandlesByContractAndDateRange(
											tradestrategy.getContract()
													.getIdContract(),
											startDate, endDate);
							if (candles.isEmpty()) {
								int result = JOptionPane.showConfirmDialog(this
										.getFrame(),
										"Candle data already exists for Symbol: "
												+ tradestrategy.getContract()
														.getSymbol()
												+ " Do you want to delete?",
										"Information",
										JOptionPane.YES_NO_OPTION);
								if (result == JOptionPane.YES_OPTION) {
									for (Candle item : candles) {
										m_tradePersistentModel
												.removeAspect(item);
									}
								} else {
									return;
								}
							}
						}
					}
				}
			}

			this.getFrame().setCursor(
					Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			this.setStatusBarMessage("Runing strategy please wait ...\n",
					BasePanel.INFORMATION);
			if (m_brokerModel.isConnected()) {
				m_menuBar.setEnabledBrokerData(false);
				m_menuBar.setEnabledRunStrategy(false);
			} else {
				m_menuBar.setEnabledTestStrategy(false);
			}
			m_menuBar.setEnabledSearchDeleteRefreshSave(false);
			cleanStrategyWorker();
			/*
			 * Now run a thread that gets and saves historical data from IB TWS.
			 */
			brokerDataRequestProgressMonitor = new BrokerDataRequestProgressMonitor(
					m_brokerModel, tradingdays);
			brokerDataRequestProgressMonitor
					.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent evt) {
							if ("progress".equals(evt.getPropertyName())) {
								int progress = (Integer) evt.getNewValue();
								setProgressBarProgress(progress,
										brokerDataRequestProgressMonitor);
							}
						}
					});
			brokerDataRequestProgressMonitor.execute();

		} catch (Exception ex) {
			this.setErrorMessage("Error running Trade Strategies.",
					ex.getMessage(), ex);
		} finally {
			this.getFrame().setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Method isStrategyWorkerRunning.
	 * @param tradingday Tradingday
	 * @return boolean
	 */
	private boolean isStrategyWorkerRunning(Tradingday tradingday) {
		for (Tradestrategy tradestrategy : tradingday.getTradestrategies()) {
			if (isStrategyWorkerRunning(tradestrategy)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method isStrategyWorkerRunning.
	 * @param tradestrategy Tradestrategy
	 * @return boolean
	 */
	private boolean isStrategyWorkerRunning(Tradestrategy tradestrategy) {

		String key = tradestrategy.getStrategy().getClassName()
				+ tradestrategy.getIdTradeStrategy();
		if (isStrategyWorkerRunning(key)) {
			return true;
		}
		key = tradestrategy.getStrategy().getStrategyManager().getClassName()
				+ tradestrategy.getIdTradeStrategy();
		if (isStrategyWorkerRunning(key)) {
			return true;
		}
		return false;
	}

	/**
	 * Method removeStrategyWorker.
	 * @param tradingday Tradingday
	 */
	private void removeStrategyWorker(Tradingday tradingday) {
		for (Tradestrategy tradestrategy : tradingday.getTradestrategies()) {
			killAllStrategyWorkersForTradestrategy(tradestrategy);
			String key = tradestrategy.getStrategy().getClassName()
					+ tradestrategy.getIdTradeStrategy();
			m_strategyWorkers.remove(key);
			key = tradestrategy.getStrategy().getStrategyManager()
					.getClassName()
					+ tradestrategy.getIdTradeStrategy();
			m_strategyWorkers.remove(key);
		}
	}

	/**
	 * Method isStrategyWorkerRunning.
	 * @param key String
	 * @return boolean
	 */
	private boolean isStrategyWorkerRunning(String key) {
		if (m_strategyWorkers.containsKey(key)) {
			StrategyRule strategy = m_strategyWorkers.get(key);
			if (!strategy.isDone()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method killStrategyWorker.
	 * @param key String
	 */
	private void killStrategyWorker(String key) {
		if (m_strategyWorkers.containsKey(key)) {
			StrategyRule strategy = m_strategyWorkers.get(key);
			if (!strategy.isDone()) {
				strategy.cancel();
			}
		}
	}

	private void killAllStrategyWorker() {
		for (String key : m_strategyWorkers.keySet()) {
			killStrategyWorker(key);
		}
	}

	private void cleanStrategyWorker() {
		for (String key : m_strategyWorkers.keySet()) {
			if (m_strategyWorkers.get(key).isDone()) {
				m_strategyWorkers.remove(key);
			}
		}
	}

	/**
	 * Method killAllStrategyWorkersForTradestrategy.
	 * @param tradestrategy Tradestrategy
	 */
	private void killAllStrategyWorkersForTradestrategy(
			Tradestrategy tradestrategy) {
		String key = null;

		key = tradestrategy.getStrategy().getClassName()
				+ tradestrategy.getIdTradeStrategy();
		if (isStrategyWorkerRunning(key)) {
			killStrategyWorker(key);
		}
		key = tradestrategy.getStrategy().getStrategyManager().getClassName()
				+ tradestrategy.getIdTradeStrategy();
		if (isStrategyWorkerRunning(key)) {
			killStrategyWorker(key);
		}
	}

	/**
	 * Method createStrategy.
	 * @param strategyClassName String
	 * @param tradestrategy Tradestrategy
	 * @throws Exception
	 */
	private synchronized void createStrategy(String strategyClassName,
			Tradestrategy tradestrategy) throws Exception {

		String key = strategyClassName + tradestrategy.getIdTradeStrategy();

		// Only allow one strategy worker per tradestrategy
		if (m_strategyWorkers.containsKey(key)) {
			StrategyRule strategy = m_strategyWorkers.get(key);
			if (strategy.isDone()) {
				m_strategyWorkers.remove(key);
				strategy = null;
				_log.info("Strategy already running and complete: "
						+ strategyClassName
						+ " Symbol: "
						+ tradestrategy.getContract().getSymbol()
						+ " seriesCount: "
						+ tradestrategy.getDatasetContainer()
								.getBaseCandleSeries().getItemCount());
			} else {
				throw new StrategyRuleException(1, 100,
						"Strategy already running: "
								+ strategyClassName
								+ " Symbol: "
								+ tradestrategy.getContract().getSymbol()
								+ " seriesCount: "
								+ tradestrategy.getDatasetContainer()
										.getBaseCandleSeries().getItemCount());
			}
		}

		Vector<Object> parm = new Vector<Object>(0);
		parm.add(m_brokerModel);
		parm.add(tradestrategy.getDatasetContainer());
		parm.add(tradestrategy.getIdTradeStrategy());

		StrategyRule strategy = (StrategyRule) dynacode.newProxyInstance(
				StrategyRule.class, StrategyRule.PACKAGE + strategyClassName,
				parm);

		strategy.addMessageListener(this);

		if (!m_brokerModel.isConnected()) {
			/*
			 * For back test the back tester listens to the strategy for orders
			 * being created/completed.
			 */
			strategy.addMessageListener(tradestrategy.getDatasetContainer()
					.getBackTestWorker());
			tradestrategy.getDatasetContainer().getBackTestWorker().execute();
		}
		strategy.execute();
		m_strategyWorkers.put(key, strategy);
		_log.info("Start: "
				+ strategyClassName
				+ " Symbol: "
				+ tradestrategy.getContract().getSymbol()
				+ " seriesCount: "
				+ tradestrategy.getDatasetContainer().getBaseCandleSeries()
						.getItemCount());
	}

	/**
	 * Method simulatedMode.
	 * @param simulated boolean
	 */
	private void simulatedMode(boolean simulated) {

		try {
			if (simulated) {
				m_brokerModel = (BrokerModel) ClassFactory
						.getServiceForInterface(BrokerModel._brokerTest, this);
				/*
				 * Controller listens for problems from the TWS interface see
				 * doError()
				 */
				m_brokerModel.addMessageListener(this);
				m_menuBar.setEnabledBrokerData(true);
				m_menuBar.setEnabledRunStrategy(false);
				m_menuBar.setEnabledTestStrategy(true);
				this.setStatusBarMessage("Running in simulated mode",
						BasePanel.INFORMATION);
			} else {
				m_menuBar.setEnabledBrokerData(true);
				m_menuBar.setEnabledRunStrategy(true);
				m_menuBar.setEnabledTestStrategy(false);
			}
		} catch (Exception ex) {
			this.setErrorMessage("Error running Simulated Mode.",
					ex.getMessage(), ex);
		}
	}

	/**
	 * Method refreshTradingdays.
	 * @param tradingdays Tradingdays
	 */
	private void refreshTradingdays(Tradingdays tradingdays) {

		for (Tradingday tradingday : tradingdays.getTradingdays().values()) {
			tradingdayPanel.doRefresh(tradingday);
		}
		if (m_brokerModel.isConnected()) {
			m_menuBar.setEnabledBrokerData(true);
			m_menuBar.setEnabledRunStrategy(true);
		} else {
			m_menuBar.setEnabledTestStrategy(true);
			cleanStrategyWorker();
		}
		m_menuBar.setEnabledSearchDeleteRefreshSave(true);
	}

	/**
	 * Method setProgressBarProgress.
	 * @param progress int
	 * @param worker SwingWorker<Void,String>
	 */
	private void setProgressBarProgress(int progress,
			SwingWorker<Void, String> worker) {

		getProgressBar().setValue(progress);
		if (getProgressBar().getMaximum() > 0) {
			String message = String.format("Completed %d%%.\n", progress);
			setStatusBarMessage(message, BasePanel.WARNING);
		}

		if (worker.isDone() || (progress == 100)) {
			Toolkit.getDefaultToolkit().beep();
			if (worker.isCancelled()) {
				setStatusBarMessage("Process canceled.\n",
						BasePanel.INFORMATION);
			} else {
				setStatusBarMessage("Process completed.\n",
						BasePanel.INFORMATION);
				getProgressBar().setMaximum(0);
				getProgressBar().setMinimum(0);
			}
		}
	}

	/**
	 */
	private class BrokerDataRequestProgressMonitor extends
			SwingWorker<Void, String> {

		private BrokerModel brokerManagerModel;
		private Tradingdays tradingdays = null;
		private int grandtotal = 0;
		private long startTime = 0;
		private final ConcurrentHashMap<Integer, Tradestrategy> m_runningContractRequests = new ConcurrentHashMap<Integer, Tradestrategy>();

		/**
		 * Constructor for BrokerDataRequestProgressMonitor.
		 * @param brokerManagerModel BrokerModel
		 * @param tradingdays Tradingdays
		 */
		public BrokerDataRequestProgressMonitor(BrokerModel brokerManagerModel,
				Tradingdays tradingdays) {
			this.brokerManagerModel = brokerManagerModel;
			this.tradingdays = tradingdays;
		}

		/**
		 * Method doInBackground.
		 * @return Void
		 */
		public Void doInBackground() {

			try {
				this.grandtotal = 0;
				for (Tradingday tradingday : this.tradingdays.getTradingdays()
						.values()) {
					this.grandtotal = this.grandtotal
							+ tradingday.getTradestrategies().size();
				}
				this.startTime = System.currentTimeMillis();
				int totalSumbitted = 0;
				// Initialize the progress bar
				getProgressBar().setMaximum(100);
				setProgress(0);
				String message = null;
				List<Date> keys = new ArrayList<Date>(this.tradingdays
						.getTradingdays().keySet());
				Collections.sort(keys);
				for (Date date : keys) {
					Tradingday tradingday = this.tradingdays.getTradingdays()
							.get(date);
					for (Tradestrategy tradestrategy : tradingday
							.getTradestrategies()) {

						if (!m_brokerModel.isRealtimeBarsRunning(tradestrategy)
								&& !m_brokerModel
										.isMarketDataRunning(tradestrategy)) {
							/*
							 * If running in test mode create the test broker
							 * client.
							 */
							if (!m_brokerModel.isConnected()) {
								BackTestBroker m_client = new BackTestBroker(
										tradestrategy.getDatasetContainer(),
										tradestrategy.getIdTradeStrategy(),
										m_brokerModel);
								tradestrategy.getDatasetContainer()
										.setBackTestWorker(m_client);
							}
							tradestrategy.getDatasetContainer()
									.getBaseCandleSeries()
									.setBarSize(tradestrategy.getBarSize());
							tradestrategy.getDatasetContainer()
									.clearBaseCandleSeries();
							/*
							 * Fire all the requests to TWS to get chart data
							 * After data has been retrieved save the data Only
							 * allow a maximum of 60 requests in a 10min period
							 * to avoid TWS pacing errors
							 */
							CandleDataset candleDataset = (CandleDataset) tradestrategy
									.getDatasetContainer().getIndicators(
											IndicatorSeries.CandleSeries);

							if (null != candleDataset) {
								for (int seriesIndex = 0; seriesIndex < candleDataset
										.getSeriesCount(); seriesIndex++) {

									Tradestrategy indicatorTradestrategy = populateChildTradestrategy(
											tradestrategy, candleDataset,
											seriesIndex);
									if (!m_indicatorTradestrategy
											.containsKey(indicatorTradestrategy
													.getIdTradeStrategy())) {
										if (m_brokerModel.isConnected()) {
											m_indicatorTradestrategy
													.put(indicatorTradestrategy
															.getIdTradeStrategy(),
															indicatorTradestrategy);
										}
										this.grandtotal++;
										if (m_brokerModel
												.isHistoricalDataRunning(indicatorTradestrategy)) {
											m_runningContractRequests
													.put(indicatorTradestrategy
															.getIdTradeStrategy(),
															indicatorTradestrategy);
										} else {
											totalSumbitted = submitBrokerRequest(
													indicatorTradestrategy,
													totalSumbitted);
										}
									}
								}
							}
							if (m_brokerModel
									.isHistoricalDataRunning(tradestrategy)) {
								m_runningContractRequests.put(
										tradestrategy.getIdTradeStrategy(),
										tradestrategy);
							} else {
								totalSumbitted = submitBrokerRequest(
										tradestrategy, totalSumbitted);
							}
						}
					}
				}
				while (!m_runningContractRequests.isEmpty()) {
					for (Integer idTradestrategy : m_runningContractRequests
							.keySet()) {
						Tradestrategy tradestrategy = m_runningContractRequests
								.get(idTradestrategy);
						if (!m_brokerModel
								.isHistoricalDataRunning(tradestrategy)) {
							totalSumbitted = submitBrokerRequest(tradestrategy,
									totalSumbitted);
							m_runningContractRequests.remove(idTradestrategy);
						}
					}
				}
				synchronized (this.brokerManagerModel.getHistoricalData()) {
					while ((this.brokerManagerModel.getHistoricalData().size() > 0)
							&& !this.isCancelled()) {
						int percent = (int) (((double) (this.grandtotal - this.brokerManagerModel
								.getHistoricalData().size()) / this.grandtotal) * 100d);
						setProgress(percent);
						this.brokerManagerModel.getHistoricalData().wait();
					}
				}
				setProgress(100);
				message = "Completed Historical data total contracts processed: "
						+ totalSumbitted
						+ " in : "
						+ ((System.currentTimeMillis() - this.startTime) / 1000)
						+ " Seconds.";
				_log.info(message);
				publish(message);
			} catch (InterruptedException ex) {
				// Do nothing
			} catch (Exception ex) {
				_log.error("Error getting history data.", ex.getMessage());
				setErrorMessage("Error getting history data.", ex.getMessage(),
						ex);
			} finally {

			}
			return null;
		}

		/**
		 * Method submitBrokerRequest.
		 * @param tradestrategy Tradestrategy
		 * @param totalSumbitted int
		 * @return int
		 * @throws InterruptedException
		 * @throws BrokerModelException
		 */
		private int submitBrokerRequest(Tradestrategy tradestrategy,
				int totalSumbitted) throws InterruptedException,
				BrokerModelException {

			m_brokerModel.onBrokerData(tradestrategy);
			totalSumbitted++;
			// _log.info("Total: " + this.grandtotal + " totalSumbitted: "
			// + totalSumbitted);
			/*
			 * Need to slow things down as limit is 60 including real time bars
			 * requests. When connected to TWS. Note only TWSManager return true
			 * for connected.
			 */
			if (((Math.floor(totalSumbitted / 58d) == (totalSumbitted / 58d)) && (totalSumbitted > 0))
					&& m_brokerModel.isConnected()) {

				// 10min - time elapsed
				int waitTime = 0;
				while ((waitTime < 601000) && !this.isCancelled()) {
					String message = "Please wait "
							+ (10 - (waitTime / 60000))
							+ " minutes as there are more than 60 data requests.\n";
					publish(message);
					waitTime = waitTime + 1000;
					Thread.sleep(1000);
				}
			}

			/*
			 * The SwingWorker has a maximum of 10 threads to run and this
			 * process uses one so we have 9 left for the BrokerWorkers. So wait
			 * while the BrokerWorkers threads complete.
			 */
			synchronized (this.brokerManagerModel.getHistoricalData()) {
				while ((this.brokerManagerModel.getHistoricalData().size() > 8)
						&& !this.isCancelled()) {
					this.brokerManagerModel.getHistoricalData().wait();
				}
			}

			int percent = (int) (((double) (totalSumbitted - this.brokerManagerModel
					.getHistoricalData().size()) / this.grandtotal) * 100d);
			setProgress(percent);
			cleanStrategyWorker();
			return totalSumbitted;
		}

		/*
		 * This method process the publish method from doInBackground().
		 */
		/**
		 * Method process.
		 * @param messages List<String>
		 */
		protected void process(List<String> messages) {
			setStatusBarMessage(messages.get(messages.size() - 1),
					BasePanel.INFORMATION);
		}

		public void done() {
			refreshTradingdays(this.tradingdays);
			String message = "Completed Historical data total contracts processed: "
					+ grandtotal
					+ " in : "
					+ ((System.currentTimeMillis() - this.startTime) / 1000)
					+ " Seconds.";
			setStatusBarMessage(message, BasePanel.INFORMATION);
		}

		/*
		 * For any child indicators that are candle based create a Tradestrategy
		 * that will get the data. If this tradestrategy already exist share
		 * this with any other tradestrategy that requires this.
		 */
		/**
		 * Method populateChildTradestrategy.
		 * @param tradestrategy Tradestrategy
		 * @param candleDataset CandleDataset
		 * @param seriesIndex int
		 * @return Tradestrategy
		 * @throws BrokerModelException
		 */
		private Tradestrategy populateChildTradestrategy(
				Tradestrategy tradestrategy, CandleDataset candleDataset,
				int seriesIndex) throws BrokerModelException {
			CandleSeries series = candleDataset.getSeries(seriesIndex);
			Tradestrategy indicatorTradestrategy = null;
			for (Tradestrategy indicator : m_indicatorTradestrategy.values()) {
				if (indicator.getContract().equals(series.getContract())
						&& indicator.getTradingday().equals(
								tradestrategy.getTradingday())
						&& indicator.getBarSize().equals(
								tradestrategy.getBarSize())
						&& indicator.getChartDays().equals(
								tradestrategy.getChartDays())
						&& indicator.getTradeAccount().equals(
								tradestrategy.getTradeAccount())) {
					indicatorTradestrategy = indicator;
					break;
				}
			}
			if (null == indicatorTradestrategy) {
				indicatorTradestrategy = new Tradestrategy(
						series.getContract(), tradestrategy.getTradingday(),
						new Strategy(), tradestrategy.getTradeAccount(),
						new BigDecimal(0), null, null, false,
						tradestrategy.getChartDays(),
						tradestrategy.getBarSize());
				indicatorTradestrategy.setIdTradeStrategy(m_brokerModel
						.getNextRequestId());
				indicatorTradestrategy.setDirty(false);
				indicatorTradestrategy.getDatasetContainer()
						.setBackTestWorker(
								tradestrategy.getDatasetContainer()
										.getBackTestWorker());
			}

			CandleSeries childSeries = indicatorTradestrategy
					.getDatasetContainer().getBaseCandleSeries();
			childSeries.setDisplaySeries(series.getDisplaySeries());
			childSeries.setSeriesRGBColor(series.getSeriesRGBColor());
			childSeries.setSymbol(series.getSymbol());
			childSeries.setSecType(series.getSecType());
			childSeries.setCurrency(series.getCurrency());
			childSeries.setExchange(series.getExchange());
			candleDataset.setSeries(seriesIndex, childSeries);
			return indicatorTradestrategy;
		}
	}

	/**
	 */
	private class DeleteProgressMonitor extends SwingWorker<Void, String> {

		private PersistentModel tradeManagerModel = null;
		private Tradingdays tradingdays = null;
		private int grandtotal = 0;
		private long startTime = 0;

		/**
		 * Constructor for DeleteProgressMonitor.
		 * @param tradeManagerModel PersistentModel
		 * @param tradingdays Tradingdays
		 */
		public DeleteProgressMonitor(PersistentModel tradeManagerModel,
				Tradingdays tradingdays) {
			this.tradingdays = tradingdays;
			this.tradeManagerModel = tradeManagerModel;
		}

		/**
		 * Method doInBackground.
		 * @return Void
		 */
		public Void doInBackground() {

			try {
				grandtotal = tradingdays.getTradingdays().size();
				this.startTime = System.currentTimeMillis();
				int totalComplete = 0;
				// Initialize the progress bar
				getProgressBar().setMaximum(100);
				setProgress(0);
				String message = null;
				for (Tradingday tradingday : tradingdays.getTradingdays()
						.values()) {
					this.tradeManagerModel.removeTradingdayTrades(tradingday);
					totalComplete++;
					int percent = (int) (((double) (totalComplete) / grandtotal) * 100d);
					setProgress(percent);
				}
				setProgress(100);
				message = "Completed delete of Trade Order data total days processed: "
						+ totalComplete
						+ " in : "
						+ ((System.currentTimeMillis() - this.startTime) / 1000)
						+ " Seconds.";
				_log.info(message);
				publish(message);
			} catch (Exception ex) {
				setErrorMessage("Error deleting Trade Orders.",
						ex.getMessage(), ex);
			}
			return null;
		}

		/*
		 * This method process the publish method from doInBackground().
		 */
		/**
		 * Method process.
		 * @param messages List<String>
		 */
		protected void process(List<String> messages) {
			setStatusBarMessage(messages.get(messages.size() - 1),
					BasePanel.INFORMATION);
		}

		public void done() {
			refreshTradingdays(this.tradingdays);
			String message = "Completed delete of Trade Order data total days processed: "
					+ grandtotal
					+ " in : "
					+ ((System.currentTimeMillis() - this.startTime) / 1000)
					+ " Seconds.";
			setStatusBarMessage(message, BasePanel.INFORMATION);
		}
	}

	/**
	 */
	private class ReAssignProgressMonitor extends SwingWorker<Void, String> {

		private PersistentModel tradeManagerModel = null;
		private Tradingdays tradingdays = null;
		private int grandtotal = 0;
		private long startTime = 0;
		private Strategy fromStrategy = null;
		private Strategy toStrategy = null;

		/**
		 * Constructor for ReAssignProgressMonitor.
		 * @param tradeManagerModel PersistentModel
		 * @param tradingdays Tradingdays
		 * @param fromStrategy Strategy
		 * @param toStrategy Strategy
		 */
		public ReAssignProgressMonitor(PersistentModel tradeManagerModel,
				Tradingdays tradingdays, Strategy fromStrategy,
				Strategy toStrategy) {
			this.tradingdays = tradingdays;
			this.tradeManagerModel = tradeManagerModel;
			this.fromStrategy = fromStrategy;
			this.toStrategy = toStrategy;
		}

		/**
		 * Method doInBackground.
		 * @return Void
		 */
		public Void doInBackground() {

			try {
				this.grandtotal = tradingdays.getTradingdays().size();
				this.startTime = System.currentTimeMillis();
				int totalComplete = 0;
				// Initialize the progress bar
				getProgressBar().setMaximum(100);
				setProgress(0);
				String message = null;
				this.toStrategy = this.tradeManagerModel
						.findStrategyById(this.toStrategy.getIdStrategy());
				for (Tradingday tradingday : tradingdays.getTradingdays()
						.values()) {
					this.tradeManagerModel.reassignStrategy(this.fromStrategy,
							this.toStrategy, tradingday);

					totalComplete++;
					int percent = (int) (((double) (totalComplete) / this.grandtotal) * 100d);
					setProgress(percent);
				}
				setProgress(100);
				message = "Complete re-assign of Strategies total days processed: "
						+ totalComplete
						+ " in : "
						+ ((System.currentTimeMillis() - this.startTime) / 1000)
						+ " Seconds.";
				_log.info(message);
				publish(message);

			} catch (Exception ex) {
				setErrorMessage("Error reassigning strategy.", ex.getMessage(),
						ex);
			}
			return null;
		}

		/*
		 * This method process the publish method from doInBackground().
		 */
		/**
		 * Method process.
		 * @param messages List<String>
		 */
		protected void process(List<String> messages) {
			setStatusBarMessage(messages.get(messages.size() - 1),
					BasePanel.INFORMATION);
		}

		public void done() {
			refreshTradingdays(this.tradingdays);
			String message = "Complete re-assign of Strategies total days processed: "
					+ grandtotal
					+ " in : "
					+ ((System.currentTimeMillis() - this.startTime) / 1000)
					+ " Seconds.";
			setStatusBarMessage(message, BasePanel.INFORMATION);
		}
	}
}