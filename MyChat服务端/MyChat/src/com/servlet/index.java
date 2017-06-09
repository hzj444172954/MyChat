package com.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import com.dao.FriendDao;
import com.dao.MessageDao;
import com.dao.RecordDao;
import com.dao.UserDao;
import com.entity.Information;
import com.entity.Record;
import com.entity.UserSession;
import com.google.gson.Gson;
import com.util.DateUtil;

/**
 * Servlet implementation class index
 */
@WebServlet("/index")
public class index extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private ServletContext application;
	private PrintWriter out;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		application = request.getServletContext();
		out = response.getWriter();
		start();
		out.println("service is started");
		System.out.println("service is started");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private void start() {
		IoAcceptor acceptor = new NioSocketAcceptor();
		// �����־������
		acceptor.getFilterChain().addLast("logger", new LoggingFilter());
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
		acceptor.setHandler(new DemoServerHandler());
		acceptor.getSessionConfig().setReadBufferSize(2048);
		acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
		try {
			acceptor.bind(new InetSocketAddress(9898));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class DemoServerHandler extends IoHandlerAdapter implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		// ��������ͻ��˴�������
		@Override
		public void sessionCreated(IoSession session) throws Exception {
			System.out.println("��������ͻ��˴�������...");
			super.sessionCreated(session);
		}

		// ��Ϣ�Ľ��մ���
		@Override
		public void messageReceived(IoSession session, Object message) throws Exception {
			Gson gson = new Gson();
			Information info = gson.fromJson(message.toString(), Information.class);
			info.setTime(DateUtil.getDate());
			String username = info.getSender();
			info.setSenderName(UserDao.getNameByUsername(username));
			System.out.println("�ͻ��� ��" + username + "�� ��" + message.toString());
			if (info.getType() == Information.PERSONAL) {

				// �û���¼������
				if (info.getExtra().equals("login")) {
					UserSession userSession = (UserSession) application.getAttribute(username);
					if (userSession != null) {

						// �Ѵ��ڸ��û�������
						if (userSession.getIMEI() != info.getMessage()) {
							// ��ͬ�û���ֻ�ֻ���¼
							Information mInfo = new Information();
							mInfo.setType(Information.PERSONAL);
							userSession.getSession().write(gson.toJson(mInfo));
						}

					} else {
						userSession = new UserSession();
					}
					userSession.setStatus(UserSession.ONLINE);
					userSession.setIMEI(info.getMessage());
					userSession.setUsername(username);
					userSession.setSession(session);
					application.setAttribute(username, userSession);
					UserDao.userLogin(username);
					System.out.println("�ͻ��� ��" + username + "�� �ĻỰ�ѱ���," + session.toString());
					// ��ȡδ����Ϣ
					List<Information> infos = MessageDao.getMessage(username);
					if (infos.size() > 0) {
						// ����δ����Ϣ
						String sender = infos.get(0).getSender();
						Information unReadInfo = new Information();
						unReadInfo.setReceiver(username);
						unReadInfo.setSender(sender);
						unReadInfo.setType(Information.SYSTEM);
						unReadInfo.setMessage(gson.toJson(infos));
						unReadInfo.setSenderName(UserDao.getNameByUsername(sender));
						unReadInfo.setSenderImg(UserDao.getImageByUsername(sender));
						unReadInfo.setTime(DateUtil.getDate());
						unReadInfo.setExtra("unReadInformation");
						System.out.println(unReadInfo.toString());
						session.write(gson.toJson(unReadInfo));
						// ���δ����Ϣ
						MessageDao.removeMessage(username);
						System.out.println("������-->" + username + "��δ����Ϣ�ѷ���(" + infos.size() + ")��");
					}
				} else if (info.getExtra().equals("logout")) {
					// �û��Ͽ�����
					UserSession userSession = (UserSession) application.getAttribute(username);
					if (userSession.getIMEI().equals(info.getMessage())) {
						application.removeAttribute(username);
						UserDao.userLogout(username);
						System.out.println("�ͻ��� ��" + username + "�� �ѶϿ�����");
					}
				}

			} else if (info.getType() == Information.FRIEND) {

				// �û���Ӻ���
				Record record = gson.fromJson(info.getMessage(), Record.class);
				System.out.println("record:" + record.toString());
				String receiver = info.getReceiver();
				int userId = UserDao.getIdByUsername(record.getUsername());
				int targetId = UserDao.getIdByUsername(record.getTargetUsername());
				if (record.getResult().equals("add")) {

					// ���
					if (RecordDao.isAddRecord(userId, targetId)) {
						// ���������Ӹ��û�
						Information info1 = new Information();
						info1.setType(Information.SYSTEM);
						info1.setExtra("hint");
						info1.setMessage("�����������Ӹú���");
						session.write(gson.toJson(info1));
					} else {
						// δ�������Ӹ��û�
						RecordDao.addRecord(userId, targetId, record.getDesc());
						Information info1 = new Information();
						info1.setType(Information.SYSTEM);
						info1.setExtra("hint");
						info1.setMessage("�����������ѷ���");
						session.write(gson.toJson(info1));
					}

				} else {

					if (record.getResult().equals("accept")) {
						if (userId > targetId) {
							FriendDao.addFriend(targetId, userId);
						} else {
							FriendDao.addFriend(userId, targetId);
						}
					}
					RecordDao.modifyRecord(record.getId(), record.getResult());

				}
				UserSession userSession = (UserSession) application.getAttribute(receiver);
				if (userSession == null) {
					// �����û�������
					MessageDao.addMessage(info);
				} else {
					userSession.getSession().write(gson.toJson(info));
				}

			}
			if (info.getType() == Information.PRIVATE) {

				String receiver = info.getReceiver();
				UserSession userSession = (UserSession) application.getAttribute(receiver);
				if (userSession == null) {
					// �����û�������
					MessageDao.addMessage(info);
				} else {
					userSession.getSession().write(gson.toJson(info));
				}

			}

		}

		@Override
		public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
			System.out.println(cause.toString());
		}

		@Override
		public void sessionClosed(IoSession session) throws Exception {
			System.out.println("sessionClosed");
		}

		@Override
		public void messageSent(IoSession session, Object message) throws Exception {
			System.out.println("messageSent:" + message.toString());
		}

	}

}
