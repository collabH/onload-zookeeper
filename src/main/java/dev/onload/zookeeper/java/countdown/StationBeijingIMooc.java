package dev.onload.zookeeper.java.countdown;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class StationBeijingIMooc extends DangerCenter {

	public StationBeijingIMooc(CountDownLatch countDown) {
		super(countDown, "北京慕课调度站");
	}

	@Override
	public void check() {
		log.warn("正在检查 [" + this.getStation() + "]...");

//		int a=1/0;
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		log.warn("检查 [" + this.getStation() + "] 完毕，可以发车~");
	}

}
