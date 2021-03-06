// pch.cpp: 与预编译标头对应的源文件；编译成功所必需的

#include "pch.h"

// 代理相关参数
SOCKET ProxyServer;
sockaddr_in ProxyServerAddr;
const int ProxyPort = 10240;

//	由于新的连接都是用新线程进行处理，对线程的频繁的创建和销毁浪费资源
// 使用线程池技术提高服务器效率：
/*
const int ProxyThreadMaxNum = 20;
HANDLE ProxyThreadHandle[ProxyThreadMaxNum] = {0};
DWORD ProxyThreadDW[ProxyThreadMaxNum] = {0};
*/


/*********************************
Method:	InitSocket
FullName:	InitSocket
Access:		public
Returns:	BOOL
Qualifier:	初始化套接字
**********************************/
BOOL InitSocket() {

	//加载套接字库
	WORD wVersionRequested;
	WSADATA wsaData;

	//套接字加载失败错误提示
	int err;

	//版本2.2
	wVersionRequested = MAKEWORD(2, 2);

	//加载dll文件 Socket库
	err = WSAStartup(wVersionRequested, &wsaData);

	// 根据返回值err，判断，若加载失败：
	if (err!=0)
	{
		//找不到 winsock.dll
		printf("加载 winsock 失败， 错误代码为：%d\n", WSAGetLastError());
		return	FALSE;
	}

	// 根据低位字节和高位字节判断版本号
	if (LOBYTE(wsaData.wVersion)!=2 || HIBYTE(wsaData.wVersion)!=2 )
	{
		printf("不能找到正确的 winsock 版本\n");
		WSACleanup();
		return	FALSE;
	}

	// 代理服务器套接字： 地址族，套接字类型，协议号
	ProxyServer = socket(AF_INET, SOCK_STREAM, 0);

	if ( INVALID_SOCKET == ProxyServer)
	{
		printf("创建套接字失败，错误代码为： %d\n", WSAGetLastError());
		return FALSE;
	}

	ProxyServerAddr.sin_family = AF_INET;	// 地址族
	ProxyServerAddr.sin_port = htons(ProxyPort); // 本地字节顺序 to 网络字节顺序
	ProxyServerAddr.sin_addr.S_un.S_addr = INADDR_ANY;	// 地址通配符

	// 绑定套接字
	if ( bind(ProxyServer, (SOCKADDR*)&ProxyServerAddr, sizeof(SOCKADDR)) == SOCKET_ERROR)
	{
		cout << "绑定套接字失败" << endl;
		return FALSE;
	}

	// 设置监听状态.	listen: 套接字，请求队列大小 -> 0：成功 ，SOCKET_ERROR：失败
	if (listen(ProxyServer , SOMAXCONN) == SOCKET_ERROR)
	{
		printf("监听端口 %d 失败",ProxyPort);
		return FALSE;
	}

return TRUE;
}



/****************************************
Method:	ProxyThread
FullName:	ProxyThread
Access:		public
Returns:	unsigned int __stdcall
Qualifier:	线程执行函数
Parameter:LPVOID lpParameter
*****************************************/
unsigned	int __stdcall ProxyThread(LPVOID lpParameter) {

	printf("\n-------------------------------------------\n");

	char Buffer[MAXSIZE];		// 缓存报文
	char *CacheBuffer;			// 缓存报文头
	ZeroMemory(Buffer, MAXSIZE);

	SOCKADDR_IN clientAddr;
	int length = sizeof(SOCKADDR_IN);
	int recvSize;
	int ret;

	// 解析http头 （由于goto限制，该初始化语句调整到goto之前）
	HttpHeader* httpHeader = new HttpHeader();


	recvSize = recv(((ProxyParam*)lpParameter)->clientSocket, Buffer, MAXSIZE, 0);
	if (recvSize <=0)
	{
		printf("error: receSize (from client) = %d\n", recvSize);
		goto error;	// 函数尾 error 标签
	}
	
	// 用于解析http头
	CacheBuffer = new char[recvSize + 1];
	ZeroMemory(CacheBuffer, recvSize + 1);
	memcpy(CacheBuffer, Buffer, recvSize);
	ParseHttpHead(CacheBuffer, httpHeader);
	delete CacheBuffer;

	// 连接代理服务器主机，host来源于http头
	if (!ConnectToServer(&((ProxyParam*)lpParameter)->serverSocket,httpHeader->host))
	{
		printf("error: Failed to connect to server.\n ");
		goto error;
	}
	
	printf("代理连接主机 %s 成功\n", httpHeader->host);

	// 将客户端发送的 HTTP 数据报文直接转发给目标服务器
	ret = send(((ProxyParam*)lpParameter)->serverSocket, Buffer, strlen(Buffer) + 1, 0	);

	// 等待目标服务器返回数据
	recvSize = recv(((ProxyParam*)lpParameter)->serverSocket, Buffer, MAXSIZE, 0);
	if (recvSize<=0)
	{
		printf("error: receSize (from server) = %d\n", recvSize);
		goto error;
	}

	// 将目标服务器返回的数据直接发给客户端
	ret = send(((ProxyParam*)lpParameter)->clientSocket, Buffer, sizeof(Buffer), 0);

	printf(">>正常");
	
// 错误处理
error:
	printf("关闭套接字\n");
	Sleep(200);
	closesocket(((ProxyParam*)lpParameter)->clientSocket);
	closesocket(((ProxyParam*)lpParameter)->serverSocket);
	delete lpParameter;
	_endthreadex(0);	

	return 0;
}



/****************************************
Method:	ParseHttpHead
FullName:	ParseHttpHead
Access:		public
Returns:	void
Qualifier:	解析TCP报文的HTTP头部
Parameter:char* buffer
Parameter:HttpHeader* httpHeader
*****************************************/
void ParseHttpHead(char* buffer, HttpHeader* httpHeader) {
	char *p;	// 分隔所得当前字符串
	char *ptr;	// 缓存分隔剩余字符串
	const char* delim = "\r\n";	// 分隔符

	p = strtok_s(buffer, delim, &ptr);	// 提取第一行, 字符串分隔
	printf("%s\n", p);

	if (p[0] == 'G')
	{
		// GET 方法
		memcpy(httpHeader->method, "GET", 3);
		memcpy(httpHeader->url,&p[4], strlen(p)-13);
	}
	else if (p[0] =='P')
	{
		// POST 方法
		memcpy(httpHeader->method, "POST", 4);
		memcpy(httpHeader->url, &p[5], strlen(p) - 14);
	}
	else
	{
		printf("The method is neither GET nor POST .\n");	// TODO 调试用
	}

	// https : CONNECT方法等，则只能获得第一行信息

	printf("%s\n", httpHeader->url);

	p = strtok_s(NULL, delim, &ptr); // 获得下一行

	while (p)
	{
		switch (p[0])
		{
		case 'H':	// Host
			memcpy(httpHeader->host, &p[6], strlen(p) - 6);
			break;

		case 'C':	// Cookie
			if (strlen(p)>8)
			{
				char header[8];
				ZeroMemory(header, sizeof(header));
				memcpy(header, p, 6);
				if (!strcmp(header,"Cookie"))
				{
					memcpy(httpHeader->cookie, &p[8], strlen(p) - 8);
				}
			}
			break;

		default:
			break;
		}

		p = strtok_s(NULL, delim, &ptr);
	}

}




/******************************************************
Method:	ConnectToServer
FullName:	ConnectToServer
Access:		public
Returns:	BOOL
Qualifier:	根据主机创建目标服务器套接字，并连接
Parameter:SOCKET* serverSocket
Parameter:char* host
******************************************************/
BOOL ConnectToServer(SOCKET* serverSocket, char * host) {
	sockaddr_in	serverAddr;
	serverAddr.sin_family = AF_INET;
	serverAddr.sin_port = htons(HTTP_PORT);
	HOSTENT *hostent = gethostbyname(host);	// 域名->32位IP地址

	if (!hostent)
	{
		return FALSE;
	}

	in_addr Inaddr = *((in_addr*)*hostent->h_addr_list);
	serverAddr.sin_addr.s_addr = inet_addr(inet_ntoa(Inaddr));

	*serverSocket = socket(AF_INET, SOCK_STREAM, 0);
	if (*serverSocket == INVALID_SOCKET)
	{
		return FALSE;
	}
	if ( connect( *serverSocket, (SOCKADDR*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR)
	{
		closesocket(*serverSocket);
		return FALSE;
	}

	return TRUE;
}


/******************************************************
Method:	
FullName:	ConnectToServer
Access:		public
Returns:	BOOL
Qualifier:	根据主机创建目标服务器套接字，并连接
Parameter:SOCKET* serverSocket
Parameter:char* host
******************************************************/