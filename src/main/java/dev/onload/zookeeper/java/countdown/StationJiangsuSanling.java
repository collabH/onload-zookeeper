package dev.onload.zookeeper.java.countdown;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class StationJiangsuSanling extends DangerCenter {

	public StationJiangsuSanling(CountDownLatch countDown) {
		super(countDown, "江苏三林调度站");
	}

	@Override
	public void check() {
		log.warn("正在检查 [" + this.getStation() + "]...");
		
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		log.warn("检查 [" + this.getStation() + "] 完毕，可以发车~");
	}

}
